package me.serce.solidity.ide.actions

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.HintAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.resolve.SolResolver
import java.nio.file.Paths

class ImportFileFix(val element: SolUserDefinedTypeName): HintAction, HighPriorityAction {
  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String =
    QuickFixBundle.message("import.class.fix")

  override fun showHint(editor: Editor): Boolean {
    val suggestion = SolResolver.resolveTypeName(element).firstOrNull()
    if (suggestion != null) {
      val importPath = buildImportPath(element.containingFile.virtualFile, suggestion.containingFile.virtualFile)
      HintManager.getInstance().showQuestionHint(editor, QuickFixBundle.message("import.class.fix") + " $importPath", element.textOffset, element.getTextRange().getEndOffset(), ImportFileAction(element.containingFile, importPath))
      return true
    } else {
      return false
    }
  }

  private fun buildImportPath(source: VirtualFile, destination: VirtualFile): String {
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

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    return SolResolver.resolveTypeName(element).isNotEmpty()
  }

  override fun getText(): String =
    QuickFixBundle.message("import.class.fix")

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {

  }
}
