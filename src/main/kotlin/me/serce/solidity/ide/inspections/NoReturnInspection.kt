package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.*

class NoReturnInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) {
        val block = o.block
        if (block != null) {
          val returns = o.returns
          if (returns != null) {
            val shouldReturn = returns.parameterDefList.any { it.name == null }
            if (shouldReturn && !block.returns) {
              holder.registerProblem(o, "no return statement")
            }
          }
        }
      }
    }
  }

  val SolBlock.returns: Boolean
    get() = this.statementList.any { it.returns }

  val SolIfStatement.returns: Boolean
    get() = if (this.statementList.size == 1) {
      false
    } else {
      statementList.all { it.returns }
    }

  val SolStatement.returns: Boolean
    get() {
      this.block.let {
        if (it != null) return it.returns
      }
      this.ifStatement.let {
        if (it != null) return it.returns
      }
      if (this.returnSt != null) {
        return true
      }
      if (this.returnTupleStatement != null) {
        return true
      }
      return false
    }
}
