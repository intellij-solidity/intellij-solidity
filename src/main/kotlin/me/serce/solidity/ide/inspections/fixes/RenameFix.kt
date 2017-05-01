package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.refactoring.RefactoringFactory
import me.serce.solidity.lang.psi.SolNamedElement


class RenameFix(val element: SolNamedElement,
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
