package me.serce.solidity.lang.types

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver

private fun getSolType(type: SolTypeName?): SolType {
  return when (type) {
    is SolElementaryTypeName -> {
      val text = type.firstChild.text
      when (text) {
        "bool" -> SolBoolean
        "string" -> SolString
        "address" -> SolAddress
        else -> {
          try {
            SolInteger.parse(text)
          } catch (e: IllegalArgumentException) {
            SolUnknown
          }
        }
      }
    }
    is SolUserDefinedTypeName -> {
      val resolvedTypes = SolResolver.resolveTypeName(type)
      return resolvedTypes.asSequence()
        .map {
          when (it) {
            is SolContractDefinition -> SolContract(it)
            is SolStructDefinition -> SolStruct(it)
            else -> null
          }
        }
        .filterNotNull()
        .firstOrElse(SolUnknown)
    }
    is SolMappingTypeName -> when {
      type.typeNameList.size >= 2 -> SolMapping(
        getSolType(type.typeNameList[0]),
        getSolType(type.typeNameList[1])
      )
      else -> SolUnknown
    }
    else -> SolUnknown
  }
}

fun inferDeclType(decl: SolNamedElement): SolType {
  return when (decl) {
    is SolVariableDeclaration -> {
      return if (decl.typeName == null || decl.typeName?.firstChild?.text == "var") {
        val parent = decl.parent
        when (parent) {
          is SolVariableDefinition -> inferExprType(parent.expression)
          else -> SolUnknown
        }
      } else
        getSolType(decl.typeName)
    }
    is SolContractDefinition -> SolContract(decl)
    is SolStructDefinition -> SolStruct(decl)
    is SolParameterDef -> getSolType(decl.typeName)
    is SolStateVariableDeclaration -> getSolType(decl.typeName)
    else -> SolUnknown
  }
}

fun inferRefType(ref: SolReferenceElement): SolType {
  return when (ref) {
    is SolVarLiteral -> {
      val declarations = SolResolver.resolveVarLiteral(ref)
      return declarations.asSequence()
        .map { inferDeclType(it) }
        .filter { it != SolUnknown }
        .firstOrElse(SolUnknown)
    }
    else -> SolUnknown
  }
}

fun inferExprType(expr: SolExpression?): SolType {
  return when (expr) {
    is SolPrimaryExpression -> {
      expr.varLiteral?.let { inferRefType(it) }
        ?: expr.booleanLiteral?.let { SolBoolean }
        ?: expr.stringLiteral?.let { SolString }
        ?: expr.numberLiteral?.let { SolInteger.INT }
        ?: SolUnknown
    }
    is SolAndExpression,
    is SolOrExpression,
    is SolEqExpression,
    is SolCompExpression -> SolBoolean

    is SolTernaryExpression -> inferExprType(expr.expressionList[1])

    else -> SolUnknown
  }
}

val SolExpression.type: SolType
  get() = CachedValuesManager.getCachedValue(this) {
    val type = RecursionManager.doPreventingRecursion(this, true) {
      inferExprType(this)
    } ?: SolUnknown
    CachedValueProvider.Result.create(type, PsiModificationTracker.MODIFICATION_COUNT)
  }
