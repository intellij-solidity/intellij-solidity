package me.serce.solidity.lang.types

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolArray.SolDynamicArray
import me.serce.solidity.lang.types.SolArray.SolStaticArray

fun getSolType(type: SolTypeName?): SolType {
  return when (type) {
    is SolBytesArrayTypeName -> {
      if (type.bytesNumType.text == "bytes") {
        SolBytes
      } else {
        SolFixedBytes.parse(type.bytesNumType.text)
      }
    }
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
      val name = type.name
      if (name != null && isInternal(name)) {
        val internalType = SolInternalTypeFactory.of(type.project).byName(name)
        return internalType ?: SolUnknown
      }
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
    is SolArrayTypeName -> {
      val sizeExpr = type.expression
      when {
        sizeExpr == null -> SolDynamicArray(getSolType(type.typeName))
        sizeExpr is SolPrimaryExpression && sizeExpr.firstChild is SolNumberLiteral ->
          SolStaticArray(getSolType(type.typeName), Integer.parseInt(sizeExpr.firstChild.text))
        else -> SolUnknown
      }
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
      } else getSolType(decl.typeName)
    }
    is SolContractDefinition -> SolContract(decl)
    is SolStructDefinition -> SolStruct(decl)
    is SolEnumDefinition -> SolEnum(decl)
    is SolEnumValue -> inferDeclType(decl.parent as SolNamedElement)
    is SolParameterDef -> getSolType(decl.typeName)
    is SolStateVariableDeclaration -> getSolType(decl.typeName)
    else -> SolUnknown
  }
}

fun inferRefType(ref: SolReferenceElement): SolType {
  return when {
    ref is SolVarLiteral && ref.name == "this" -> {
      findContract(ref)
        ?.let { SolContract(it) } ?: SolUnknown
    }
    ref is SolVarLiteral && ref.name == "super" -> SolUnknown
    ref is SolVarLiteral -> {
      val declarations = SolResolver.resolveVarLiteral(ref)
      return declarations.asSequence()
        .map { inferDeclType(it) }
        .filter { it != SolUnknown }
        .firstOrElse(SolUnknown)
    }
    else -> SolUnknown
  }
}

fun findContract(element: PsiElement): SolContractDefinition? =
  element.ancestors
    .filterIsInstance<SolContractDefinition>()
    .firstOrNull()

fun inferExprType(expr: SolExpression?): SolType {
  return when (expr) {
    is SolPrimaryExpression -> {
      expr.varLiteral?.let { inferRefType(it) }
        ?: expr.booleanLiteral?.let { SolBoolean }
        ?: expr.stringLiteral?.let { SolString }
        ?: expr.numberLiteral?.let { SolInteger.INT }
        ?: expr.elementaryTypeName?.let { getSolType(it) }
        ?: SolUnknown
    }
    is SolAndExpression,
    is SolOrExpression,
    is SolEqExpression,
    is SolCompExpression -> SolBoolean
    is SolTernaryExpression -> inferExprType(expr.expressionList[1])
    is SolIndexAccessExpression -> {
      val arrType = inferExprType(expr.expressionList[0])
      when (arrType) {
        is SolArray -> arrType.type
        is SolMapping -> arrType.to
        else -> SolUnknown
      }
    }
    is SolMemberAccessExpression -> {
      val properties = SolResolver.resolveMemberAccess(expr)
      return properties.asSequence()
        .map { inferDeclType(it) }
        .filter { it != SolUnknown }
        .firstOrElse(SolUnknown)
    }
    else -> SolUnknown
  }
}

val SolExpression.type: SolType
  get() {
    if (!isValid) {
      return SolUnknown
    }
    return CachedValuesManager.getCachedValue(this) {
      val type = RecursionManager.doPreventingRecursion(this, true) {
        inferExprType(this)
      } ?: SolUnknown
      CachedValueProvider.Result.create(type, PsiModificationTracker.MODIFICATION_COUNT)
    }
  }
