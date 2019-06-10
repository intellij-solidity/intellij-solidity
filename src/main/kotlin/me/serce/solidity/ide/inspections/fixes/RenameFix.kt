package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringFactory

class RenameFix(
  element: PsiElement,
  private val newName: String,
  private val fixName: String = "Rename to '$newName'"
) : LocalQuickFixOnPsiElement(element) {
  override fun getText() = fixName
  override fun getFamilyName() = "Rename element"

  override fun invoke(project: Project, file: PsiFile, element: PsiElement, endElement: PsiElement) {
    RefactoringFactory.getInstance(project).createRename(element, newName).run()
  }

  override fun startInWriteAction() = false
}
