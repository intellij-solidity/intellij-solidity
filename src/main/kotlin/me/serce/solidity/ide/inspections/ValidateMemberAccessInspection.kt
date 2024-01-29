package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.SolMemberAccessExpression
import me.serce.solidity.lang.psi.SolVisitor
import me.serce.solidity.lang.resolve.SolResolver

class ValidateMemberAccessInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitMemberAccessExpression(element: SolMemberAccessExpression) {
        val id = element.identifier ?: return
        val refs = SolResolver.resolveMemberAccess(element)
        when {
          refs.isEmpty() -> holder.registerProblem(id, "Member cannot be resolved")
//            refs.size > 1 -> holder.registerProblem(element.parents(true).first { it.textLength > 0 }, "Multiple members resolved")
        }
      }
    }
  }
}
