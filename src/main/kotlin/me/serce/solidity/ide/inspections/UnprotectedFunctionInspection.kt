package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.*

/**
 * https://twitter.com/flyosity/status/887866459896123392
 */
class UnprotectedFunctionInspection : LocalInspectionTool() {
  companion object {
    const val MESSAGE = "Function might modify owners of the contract, but has no modifiers. Consider adding an explicit modifier."
  }

  override fun getDisplayName() = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) {
        inspectFunctionDefinition(o, holder)
      }
    }
  }

  private fun inspectFunctionDefinition(funDef: SolFunctionDefinition, holder: ProblemsHolder) {
    if (funDef.modifiers.isEmpty() && funDef.functionVisibilitySpecifierList.isEmpty() && !funDef.isConstructor) {
      for (statement in funDef.block?.statementList ?: emptyList()) {
        val expression = statement.expression
        if (expression is SolAssignmentExpression) {
          val left = expression.expressionList.first()
          if (maybeOwnerReference(left)) {
            val element = funDef.identifier ?: funDef
            holder.registerProblem(element, MESSAGE)
            return
          }
        }
      }
    }
  }

  private fun maybeOwnerReference(ref: SolElement): Boolean {
    when (ref) {
      is SolPrimaryExpression -> {
        val literal = ref.varLiteral
        if (literal != null) {
          return maybeOwnerReference(literal)
        }
      }
      is SolIndexAccessExpression -> {
        val accessExpr = ref.expressionList.first()
        if (accessExpr != null) {
          return maybeOwnerReference(accessExpr)
        }
      }
      is SolNamedElement -> {
        val name = ref.name
        if (name != null && name.contains("owner")) {
          return true
        }
      }
    }
    return false
  }
}
