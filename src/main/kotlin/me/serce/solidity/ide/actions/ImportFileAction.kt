package me.serce.solidity.ide.actions

import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.psi.*

class ImportFileAction(private val file: PsiFile,
                       private val importPath: String) : QuestionAction {

  override fun execute(): Boolean {
    val project = file.project
    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        val after = file.children.filterIsInstance<SolImportDirective>().lastOrNull() ?: file.children.filterIsInstance<SolPragmaDirective>().firstOrNull()
        val factory = SolPsiFactory(project)
        file.addAfter(factory.createImportDirective(importPath), after)
        file.addAfter(factory.createNewLine(project), after)
      }
    }
    return true
  }
}
