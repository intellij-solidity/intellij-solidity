package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolStateVariableDeclaration
import me.serce.solidity.lang.psi.SolVisitor

class VisibilityModifierInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) {
        if (o.visibility == null) {
          registerProblem((o.identifier ?: return))
        }
      }

      override fun visitStateVariableDeclaration(o: SolStateVariableDeclaration) {
        if (o.visibility == null) {
          registerProblem((o.identifier))
        }
      }

      private fun registerProblem(id: PsiElement) {
        holder.registerProblem(id, "No visibility modifier", ProblemHighlightType.WEAK_WARNING)
      }
    }
  }
}
