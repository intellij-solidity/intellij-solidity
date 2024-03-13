package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.SolMemberAccessExpression
import me.serce.solidity.lang.psi.SolVisitor
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolUnknown
import me.serce.solidity.lang.types.type

class ValidateMemberAccessInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitMemberAccessExpression(element: SolMemberAccessExpression) {
        val id = element.identifier ?: return
        val refs = SolResolver.resolveMemberAccess(element)
        if (refs.isEmpty() && element.expression.type != SolUnknown) {
          holder.registerProblem(id, "Member cannot be resolved")
        }
      }
    }
  }
}
