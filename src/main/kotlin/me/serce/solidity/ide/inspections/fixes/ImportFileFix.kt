package me.serce.solidity.ide.inspections.fixes

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

class ImportFileFix(element: SolUserDefinedTypeName) : LocalQuickFixOnPsiElement(element), HintAction, LocalQuickFix {
  override fun startInWriteAction(): Boolean = false

  override fun getFamilyName(): String = "Import file"

  override fun showHint(editor: Editor): Boolean {
    val element = startElement as SolUserDefinedTypeName?
    if (element != null) {
      val suggestions = SolResolver.resolveTypeName(element).map { it.containingFile }.toSet()
      val fixText: String? = when {
          suggestions.size == 1 -> {
            val importPath = buildImportPath(element.containingFile.virtualFile, suggestions.first().virtualFile)
            "$familyName $importPath"
          }
          suggestions.isNotEmpty() -> familyName
          else -> null
      }
      return when {
        fixText != null -> {
          HintManager.getInstance().showQuestionHint(editor, fixText, element.textOffset, element.textRange.endOffset,
            ImportFileAction(editor, element.containingFile, suggestions)
          )
          true
        }
        else -> false
      }
    } else {
      return false
    }
  }

  override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
    val element = startElement as SolUserDefinedTypeName?
    return when {
        element != null -> when {
            !element.isValid || element.reference?.resolve() != null -> false
            else -> SolResolver.resolveTypeName(element).isNotEmpty()
        }
        else -> false
    }
  }

  override fun getText(): String = familyName

  override fun invoke(project: Project, file: PsiFile, element: PsiElement, endElement: PsiElement) {
    val suggestions = SolResolver.resolveTypeName(element as SolReferenceElement).map { it.containingFile }.toSet()
    if (suggestions.size == 1) {
      ImportFileAction.addImport(project, file, suggestions.first())
    }
  }

  override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
    val element = startElement as SolUserDefinedTypeName?
    if (element != null) {
      val suggestions = SolResolver.resolveTypeName(element as SolReferenceElement).map { it.containingFile }.toSet()
      if (suggestions.size == 1) {
        ImportFileAction.addImport(project, element.containingFile, suggestions.first())
      }
    }
  }
}
