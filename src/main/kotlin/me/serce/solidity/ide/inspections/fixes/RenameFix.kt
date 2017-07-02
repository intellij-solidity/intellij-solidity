package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringFactory


class RenameFix(val element: PsiElement,
                val newName: String,
                val fixName: String = "Rename to '$newName'") : LocalQuickFix {
  override fun getName() = fixName
  override fun getFamilyName() = "Rename element"

  override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
    ApplicationManager.getApplication().invokeLater {
      RefactoringFactory.getInstance(project).createRename(element, newName).run()
    }
  }
}
