package me.serce.solidity.ide.actions

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.psi.SolImportDirective
import me.serce.solidity.lang.psi.SolPragmaDirective
import me.serce.solidity.lang.psi.SolPsiFactory

class ImportFileAction(private val file: PsiFile, private val importPath: String) : QuestionAction {
  override fun execute(): Boolean {
    val project = file.project
    CommandProcessor.getInstance().executeCommand(project, { ApplicationManager.getApplication().runWriteAction {
      val after = file.children.filterIsInstance<SolImportDirective>().lastOrNull() ?: file.children.filterIsInstance<SolPragmaDirective>().firstOrNull()
      file.addAfter(SolPsiFactory(project).createImportDirective(importPath), after)
    } }, QuickFixBundle.message("add.import"), null)
    return true
  }
}
