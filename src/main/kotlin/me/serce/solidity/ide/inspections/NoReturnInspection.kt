package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.*

class NoReturnInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) {
        o.inspectReturns(holder)
      }
    }
  }
}

fun SolFunctionDefinition.inspectReturns(holder: ProblemsHolder) {
  val block = this.block
  if (block != null) {
    val returns = this.returns
    if (returns != null) {
      if (!block.returns) {
        val shouldReturn = returns.parameterDefList.any { it.name == null }
        if (shouldReturn) {
          holder.registerProblem(this, "no return statement")
        } else {
          for (returnParam in returns.parameterDefList) {
            if (!block.hasAssignment(returnParam.name!!)) {
              holder.registerProblem(returnParam, "return variable not assigned")
            }
          }
        }
      }
    }
  }
}

fun SolStatement.hasAssignment(name: String): Boolean {
  this.variableDefinition.let {
    if (it != null) return it.hasAssignment(name)
  }

  this.expression.let {
    if (it != null && it is SolAssignmentExpression) {
      val first = it.expressionList[0]
      if (first is SolPrimaryExpression) {
        first.varLiteral.let {lit ->
          if (lit != null) {
            if (lit.name == name) {
              return true
            }
          }
        }
      }
      return false
    }
  }

  this.block.let {
    if (it != null) return it.hasAssignment(name)
  }

  this.ifStatement.let {
    if (it != null) return it.hasAssignment(name)
  }

  return false
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

val SolBlock.returns: Boolean
  get() = this.statementList.any { it.returns }

fun SolBlock.hasAssignment(name: String): Boolean =
  this.statementList.any { it.hasAssignment(name) }

val SolIfStatement.returns: Boolean
  get() = if (this.statementList.size == 1) {
    false
  } else {
    statementList.all { it.returns }
  }

fun SolIfStatement.hasAssignment(name: String): Boolean =
  if (this.statementList.size == 1) {
    false
  } else {
    statementList.all { it.hasAssignment(name) }
  }

fun SolVariableDefinition.hasAssignment(name: String): Boolean =
  this.variableDeclaration.name == name
