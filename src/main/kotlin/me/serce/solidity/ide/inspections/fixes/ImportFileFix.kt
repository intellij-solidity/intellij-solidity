package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInspection.HintAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.serce.solidity.ide.inspections.fixes.ImportFileAction.Companion.buildImportPath
import me.serce.solidity.lang.psi.SolReferenceElement
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.resolve.SolResolver

class ImportFileFix(element: SolUserDefinedTypeName): LocalQuickFixOnPsiElement(element), HintAction, LocalQuickFix {
  val element:SolUserDefinedTypeName
    get() = startElement as SolUserDefinedTypeName

  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String =
    QuickFixBundle.message("import.class.fix")

  override fun showHint(editor: Editor): Boolean {
    val suggestions = SolResolver.resolveTypeName(element).map { it.containingFile }.toSet()
    val fixText: String? = if (suggestions.size == 1) {
      val importPath = buildImportPath(element.containingFile.virtualFile, suggestions.first().virtualFile)
      QuickFixBundle.message("import.class.fix") + " $importPath"
    } else if (suggestions.isNotEmpty()) {
      QuickFixBundle.message("import.class.fix")
    } else {
      null
    }
    if (fixText != null) {
      HintManager.getInstance().showQuestionHint(editor, fixText, element.textOffset, element.getTextRange().getEndOffset(), ImportFileAction(editor, element.containingFile, suggestions))
      return true
    } else {
      return false
    }
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    if (!element.isValid) {
      return false
    }
    if (element.reference?.resolve() != null) {
      return false
    }
    return SolResolver.resolveTypeName(element).isNotEmpty()
  }

  override fun getText(): String =
    QuickFixBundle.message("import.class.fix")

  override fun invoke(project: Project, file: PsiFile, element: PsiElement, endElement: PsiElement) {
    val suggestions = SolResolver.resolveTypeName(element as SolReferenceElement).map { it.containingFile }.toSet()
    if (suggestions.size == 1) {
      ImportFileAction.addImport(project, file, suggestions.first())
    }
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val suggestions = SolResolver.resolveTypeName(element as SolReferenceElement).map { it.containingFile }.toSet()
    if (suggestions.size == 1) {
      ImportFileAction.addImport(project, element.containingFile, suggestions.first())
    }
  }
}
