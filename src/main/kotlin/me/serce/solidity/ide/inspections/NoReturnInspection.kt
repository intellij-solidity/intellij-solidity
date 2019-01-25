package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.types.SolInternalTypeFactory
import me.serce.solidity.lang.types.findContract

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
    return if (this.name == "revert") {
      val ref = this.reference?.resolve()
      ref?.isGlobal() ?: false
    } else {
      false
    }
  }

private fun SolStatement.hasAssignment(el: SolNamedElement): Boolean {
  this.throwSt?.let {
    return true
  }

  this.variableDefinition?.let {
    return it.hasAssignment(el)
  }

  this.expression?.let {
    if (it is SolAssignmentExpression) {
      return it.expressionList[0].isReferenceTo(el)
    } else if (it is SolFunctionCallExpression && it.revert) {
      return true
    }
  }

  this.inlineAssemblyStatement.let { st ->
    st?.assemblyBlock?.let {
      return it.hasAssignment(el)
    }
  }

  this.block?.let {
    return it.hasAssignment(el)
  }

  this.ifStatement?.let {
    return it.hasAssignment(el)
  }

  this.tupleStatement?.let { st ->
    val declaration = st.variableDeclaration
    val expressions = st.expressionList
    if (declaration != null) {
      return declaration.declarationList?.declarationItemList?.any { it.hasAssignment(el) }
        ?: declaration.typedDeclarationList?.typedDeclarationItemList?.any { it.hasAssignment(el) }
        ?: false
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
    this.varLiteral?.let { lit ->
      if (lit.reference?.resolve() == el) {
        return true
      }
    }
  }
  return false
}

private val SolStatement.returns: Boolean
  get() {
    this.expression?.let {
      if (it is SolFunctionCallExpression && it.revert) {
        return true
      }
    }

    this.inlineAssemblyStatement.let { st ->
      st?.assemblyBlock?.let {
        return it.returns
      }
    }

    this.throwSt?.let {
      return true
    }

    this.block?.let {
      return it.returns
    }

    this.ifStatement?.let {
      return it.returns
    }

    if (this.returnSt != null) {
      return true
    }

    if (this.returnTupleStatement != null) {
      return true
    }

    return false
  }

private val SolAssemblyItem.returns: Boolean
  get() {
    functionalAssemblyExpression?.let {
      return it.text.startsWith("return")//todo better
    }
    return false
  }

private val SolAssemblyBlock.returns: Boolean
  get() {
    return this.assemblyItemList.any { it.returns }
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
  this.assemblyAssignment?.let {
    if (it.identifier?.text == el.name) {//todo better
      return true
    }
  }
  this.functionalAssemblyAssignment?.let {
    if (it.identifierOrList.text == el.name) {//todo better
      return true
    }
  }
  return false
}

private fun SolDeclarationItem.hasAssignment(el: SolNamedElement): Boolean {
  return this.identifier?.text == el.name
}

private fun SolTypedDeclarationItem.hasAssignment(el: SolNamedElement): Boolean {
  return this.identifier?.text == el.name
}

private fun SolElement.isGlobal(): Boolean {
  val contract = findContract(this)
  return contract == SolInternalTypeFactory.of(this.project).globalType.ref
}
