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

private fun SolFunctionDefinition.inspectReturns(holder: ProblemsHolder) {
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
            if (!block.hasAssignment(returnParam)) {
              holder.registerProblem(returnParam, "return variable not assigned")
            }
          }
        }
      }
    }
  }
}

private val SolFunctionCallExpression.revert: Boolean
  get() {
    return this.name == "revert"
      && expressionList.isEmpty()
      && this.reference?.resolve() == null
  }

private fun SolStatement.hasAssignment(el: SolNamedElement): Boolean {
  this.throwSt.let {
    if (it != null && it is SolFunctionCallExpression && it.revert) return true
  }

  this.variableDefinition.let {
    if (it != null) return it.hasAssignment(el)
  }

  this.expression.let {
    if (it != null && it is SolAssignmentExpression) {
      return it.expressionList[0].isReferenceTo(el)
    }
  }

  this.inlineAssemblyStatement.let { st ->
    st?.assemblyBlock?.let {
      return it.hasAssignment(el)
    }
  }

  this.block.let {
    if (it != null) return it.hasAssignment(el)
  }

  this.ifStatement.let {
    if (it != null) return it.hasAssignment(el)
  }

  this.tupleStatement?.let { st ->
    val declaration = st.variableDeclaration
    val expressions = st.expressionList
    if (declaration != null) {

    } else {
      expressions.firstOrNull()?.let {
        if (it is SolSeqExpression) {
          return it.expressionList.any { expr -> expr.isReferenceTo(el) }
        }
      }
    }
  }

  return false
}

private fun SolExpression.isReferenceTo(el: SolNamedElement): Boolean {
  if (this is SolPrimaryExpression) {
    this.varLiteral.let { lit ->
      if (lit != null) {
        if (lit.reference?.resolve() == el) {
          return true
        }
      }
    }
  }
  return false
}

private val SolStatement.returns: Boolean
  get() {
    this.expression.let {
      if (it != null && it is SolFunctionCallExpression && it.revert) {
        return true
      }
    }

    this.throwSt.let {
      if (it != null) return true
    }

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

private val SolBlock.returns: Boolean
  get() = this.statementList.any { it.returns }

private fun SolBlock.hasAssignment(el: SolNamedElement): Boolean =
  this.statementList.any { it.hasAssignment(el) }

private val SolIfStatement.returns: Boolean
  get() = if (this.statementList.size == 1) {
    false
  } else {
    statementList.all { it.returns }
  }

private fun SolIfStatement.hasAssignment(el: SolNamedElement): Boolean =
  if (this.statementList.size == 1) {
    false
  } else {
    statementList.all { it.hasAssignment(el) }
  }

private fun SolVariableDefinition.hasAssignment(el: SolNamedElement): Boolean =
  this.variableDeclaration.reference?.resolve() == el

private fun SolAssemblyBlock.hasAssignment(el: SolNamedElement): Boolean =
  this.assemblyItemList.any { it.hasAssignment(el) }

private fun SolAssemblyItem.hasAssignment(el: SolNamedElement): Boolean {
  this.assemblyAssignment.let {
    if (it != null && it.identifier?.text == el.name) {//todo better
      return true
    }
  }
  this.functionalAssemblyAssignment.let {
    if (it != null && it.identifierOrList.text == el.name) {//todo better
      return true
    }
  }
  return false
}
