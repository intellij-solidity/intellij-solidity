package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import me.serce.solidity.ide.formatting.SolImportOptimizer
import me.serce.solidity.lang.resolve.ref.SolImportConfigService
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolImportDirective
import me.serce.solidity.lang.psi.SolPragmaDirective
import me.serce.solidity.lang.psi.SolPsiFactory
import me.serce.solidity.nullIfError
import java.awt.BorderLayout
import java.io.File
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.ListCellRenderer


class ImportFileAction(
  val editor: Editor,
  private val file: PsiFile,
  private val suggestions: Set<PsiFile>
) : QuestionAction {

  val project: Project
    get() = file.project


  override fun execute(): Boolean {
    PsiDocumentManager.getInstance(project).commitAllDocuments()

    if (suggestions.size == 1) {
      addImport(project, file, suggestions.first())
    } else {
      chooseFileToImport()
    }

    return true
  }

  private fun chooseFileToImport() {
    val files = suggestions.map { it.virtualFile }.distinct()
    val step = object : BaseListPopupStep<VirtualFile>("File to import", files.toMutableList()) {
      override fun isAutoSelectionEnabled(): Boolean {
        return false
      }

      override fun isSpeedSearchEnabled(): Boolean {
        return true
      }

      override fun onChosen(selectedValue: VirtualFile?, finalChoice: Boolean): PopupStep<*>? {
        if (selectedValue == null) {
          return PopupStep.FINAL_CHOICE
        }

        return doFinalStep {
          PsiDocumentManager.getInstance(project).commitAllDocuments()
          val psi = PsiManager.getInstance(project).findFile(selectedValue)
          if (psi == null) {
            return@doFinalStep
          }
          addImport(project, file, psi)
        }
      }

      override fun hasSubstep(selectedValue: VirtualFile?): Boolean {
        return true
      }

      override fun getTextFor(value: VirtualFile): String {
        return value.name
      }

      override fun getIconFor(aValue: VirtualFile): Icon? {
        return aValue.fileType.icon
      }
    }

    val popup = object : ListPopupImpl(project, step) {
      override fun getListElementRenderer(): ListCellRenderer<VirtualFile> {
        @Suppress("UNCHECKED_CAST")
        val baseRenderer = super.getListElementRenderer() as PopupListElementRenderer<VirtualFile>
        return ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
          val panel = JPanel(BorderLayout())
          baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          panel.add(baseRenderer.nextStepLabel, BorderLayout.EAST)
          panel.add(baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus))
          panel
        }
      }
    }
    popup.showInBestPositionFor(editor)
  }

  companion object {
    fun isImportedAlready(file: PsiFile, to: PsiFile): Boolean {
      return if (file == to) {
        true
      } else {
        RecursionManager.doPreventingRecursion(file, true) {
          file.children
            .filterIsInstance<SolImportDirective>()
            .mapNotNull { nullIfError { it.importPath?.reference?.resolve()?.containingFile } }
            .any {
              isImportedAlready(it, to)
            }
        } ?: false
      }
    }

    fun addContractImport(contract: SolContractDefinition, file: PsiFile) {
      CommandProcessor.getInstance().runUndoTransparentAction {
        ApplicationManager.getApplication().runWriteAction {
          val after = file.children.filterIsInstance<SolImportDirective>().lastOrNull()
            ?: file.children.filterIsInstance<SolPragmaDirective>().firstOrNull()
          val factory = SolPsiFactory(contract.project)
          val contractName = contract.name;
          val path = buildImportPath(contract.project, file.virtualFile, contract.containingFile.virtualFile);
          if (contractName.isNullOrBlank()) {
            file.addAfter(factory.createImportDirective(path), after);
          } else {
            file.addAfter(factory.createContractImportFromDirective(path, contractName), after);
          }

          file.addAfter(factory.createNewLine(contract.project), after)
        }
      }
    }

    fun addImport(project: Project, file: PsiFile, to: PsiFile) {
      CommandProcessor.getInstance().runUndoTransparentAction {
        ApplicationManager.getApplication().runWriteAction {
          val after = file.children.filterIsInstance<SolImportDirective>().lastOrNull()
            ?: file.children.filterIsInstance<SolPragmaDirective>().lastOrNull()
          val factory = SolPsiFactory(project)
          file.addAfter(factory.createImportDirective(buildImportPath(project, file.virtualFile, to.virtualFile)), after)
          file.addAfter(factory.createNewLine(project), after)
          SolImportOptimizer().processFile(file, false).run()
        }
      }
    }

    fun createImport(factory: SolPsiFactory, solUserDefinedTypeName: List<String>, file: VirtualFile, to: VirtualFile): SolImportDirective {
      val content = "${(solUserDefinedTypeName.takeIf { it.isNotEmpty() }?.let { "{${it.joinToString(", ")}} from " } ?: "")}\"${buildImportPath(factory.project, to, file)}\""
      return factory.createImportDirective(content, false)
    }

    fun buildImportPath(project: Project, source: VirtualFile, destination: VirtualFile): String {
      return Paths.get(source.path).parent.relativize(Paths.get(destination.path)).toString().let { importPath ->
        val separator = File.separator

        // Try reverse remappings first -- works for lib/, dependencies/, or any custom target
        val mapping = SolImportConfigService.getInstance(project).reverseRemappings(source)
        val reverseMatched = mapping.keys.firstOrNull { importPath.contains(it) }
          ?.let { importPath.substring(importPath.indexOf(it)).replaceFirst(it, mapping[it]!!) }

        when {
            reverseMatched != null -> reverseMatched

            importPath.contains("node_modules$separator") -> {
              val idx = importPath.indexOf("node_modules$separator")
              importPath.substring(idx + "node_modules$separator".length)
            }

            importPath.contains("installed_contracts$separator") -> {
              val idx = importPath.indexOf("installed_contracts$separator")
              importPath.substring(idx + "installed_contracts$separator".length)
                .replaceFirst("${separator}contracts${separator}", separator)
            }

            !importPath.startsWith(".") -> ".$separator$importPath"
            else -> importPath
        }
      }.replace("\\", "/")
    }
  }
}
