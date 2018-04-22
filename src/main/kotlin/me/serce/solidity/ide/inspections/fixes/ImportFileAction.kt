package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import me.serce.solidity.lang.psi.SolImportDirective
import me.serce.solidity.lang.psi.SolPragmaDirective
import me.serce.solidity.lang.psi.SolPsiFactory
import java.awt.BorderLayout
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
    val step = object : BaseListPopupStep<PsiFile>("File to import", suggestions.toMutableList()) {
      override fun isAutoSelectionEnabled(): Boolean {
        return false
      }

      override fun isSpeedSearchEnabled(): Boolean {
        return true
      }

      override fun onChosen(selectedValue: PsiFile?, finalChoice: Boolean): PopupStep<*>? {
        if (selectedValue == null) {
          return PopupStep.FINAL_CHOICE
        }

        return doFinalStep {
          PsiDocumentManager.getInstance(project).commitAllDocuments()
          addImport(project, file, selectedValue)
        }
      }

      override fun hasSubstep(selectedValue: PsiFile?): Boolean {
        return true
      }

      override fun getTextFor(value: PsiFile): String {
        return value.name
      }

      override fun getIconFor(aValue: PsiFile): Icon? {
        return aValue.getIcon(0)
      }
    }

    val popup = object : ListPopupImpl(step) {
      override fun getListElementRenderer(): ListCellRenderer<PsiFile> {
        val baseRenderer = super.getListElementRenderer() as PopupListElementRenderer<PsiFile>
        val psiRenderer = DefaultPsiElementCellRenderer()
        return ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
          val panel = JPanel(BorderLayout())
          baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          panel.add(baseRenderer.nextStepLabel, BorderLayout.EAST)
          panel.add(psiRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus))
          panel
        }
      }
    }
    popup.showInBestPositionFor(editor)
  }

  companion object {
    fun addImport(project: Project, file: PsiFile, to: PsiFile) {
      CommandProcessor.getInstance().runUndoTransparentAction {
        ApplicationManager.getApplication().runWriteAction {
          val after = file.children.filterIsInstance<SolImportDirective>().lastOrNull() ?: file.children.filterIsInstance<SolPragmaDirective>().firstOrNull()
          val factory = SolPsiFactory(project)
          file.addAfter(factory.createImportDirective(buildImportPath(file.virtualFile, to.virtualFile)), after)
          file.addAfter(factory.createNewLine(project), after)
        }
      }
    }

    fun buildImportPath(source: VirtualFile, destination: VirtualFile): String {
      return Paths.get(source.path).parent.relativize(Paths.get(destination.path)).toString().let {
        if (it.contains("node_modules/")) {
          val idx = it.indexOf("node_modules/")
          it.substring(idx + "node_modules/".length)
        } else if (!it.startsWith(".")) {
          "./$it"
        } else {
          it
        }
      }
    }
  }
}
