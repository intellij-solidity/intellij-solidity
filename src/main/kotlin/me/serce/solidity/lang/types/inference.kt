package me.serce.solidity.lang.types

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.types.SolArray.SolDynamicArray
import me.serce.solidity.lang.types.SolArray.SolStaticArray
import kotlin.math.max

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
      when (val text = type.firstChild.text) {
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
    is SolUserDefinedLocationTypeName ->
      type.userDefinedTypeName?.let { getSolTypeFromUserDefinedTypeName(it) } ?: SolUnknown
    is SolUserDefinedTypeName -> getSolTypeFromUserDefinedTypeName(type)
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

private fun getSolTypeFromUserDefinedTypeName(type: SolUserDefinedTypeName): SolType {
  val name = type.name
  if (name != null && isInternal(name)) {
    val internalType = SolInternalTypeFactory.of(type.project).byName(name)
    return internalType ?: SolUnknown
  }
  val resolvedTypes = SolResolver.resolveTypeNameUsingImports(type)
  return resolvedTypes.asSequence()
    .map {
      when (it) {
        is SolContractDefinition -> SolContract(it)
        is SolStructDefinition -> SolStruct(it)
        is SolEnumDefinition -> SolEnum(it)
        else -> null
      }
    }
    .filterNotNull()
    .firstOrElse(SolUnknown)
}

fun inferDeclType(decl: SolNamedElement): SolType {
  return when (decl) {
    is SolDeclarationItem -> {
      val list = decl.findParent<SolDeclarationList>()
      val def = list.findParent<SolVariableDefinition>()
      val inferred = inferExprType(def.expression)
      val index = list.declarationItemList.indexOf(decl)
      when (inferred) {
        is SolTuple -> inferred.types[index]
        else -> SolUnknown
      }
    }
    is SolTypedDeclarationItem -> getSolType(decl.typeName)
    is SolVariableDeclaration -> {
      return if (decl.typeName == null || decl.typeName?.firstChild?.text == "var") {
        when (val parent = decl.parent) {
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

fun inferRefType(ref: SolVarLiteral): SolType {
  return when (ref.name) {
    "this" -> {
      ref.findContract()
        ?.let { SolContract(it) } ?: SolUnknown
    }
    "super" -> SolUnknown
    else -> {
      val declarations = SolResolver.resolveVarLiteral(ref)
      return declarations.asSequence()
        .map { inferDeclType(it) }
        .filter { it != SolUnknown }
        .firstOrElse(SolUnknown)
    }
  }
}

inline fun <reified T : PsiElement> PsiElement.findParent(): T {
  return this.ancestors
    .filterIsInstance<T>()
    .first()
}

inline fun <reified T : PsiElement> PsiElement.findParentOrNull(): T? {
  return this.ancestors
    .filterIsInstance<T>()
    .firstOrNull()
}

fun PsiElement.findContract(): SolContractDefinition? = this.findParentOrNull()

fun inferExprType(expr: SolExpression?): SolType {
  return when (expr) {
    is SolPrimaryExpression -> {
      expr.varLiteral?.let { inferRefType(it) }
        ?: expr.booleanLiteral?.let { SolBoolean }
        ?: expr.stringLiteral?.let { SolString }
        ?: expr.numberLiteral?.let { SolInteger.inferType(it) }
        ?: expr.elementaryTypeName?.let { getSolType(it) }
        ?: SolUnknown
    }
    is SolPlusMinExpression -> getNumericExpressionType(
      inferExprType(expr.expressionList[0]),
      inferExprType(expr.expressionList[1])
    )
    is SolMultDivExpression -> getNumericExpressionType(
      inferExprType(expr.expressionList[0]),
      inferExprType(expr.expressionList[1])
    )
    is SolExponentExpression -> getNumericExpressionType(
      inferExprType(expr.expressionList[0]),
      inferExprType(expr.expressionList[1])
    )
    is SolFunctionCallExpression -> {
      (expr.reference as SolFunctionCallReference)
        .resolveFunctionCall()
        .firstOrNull { it.canBeApplied(expr.functionCallArguments) }
        ?.parseType()
        ?: SolUnknown
    }
    is SolAndExpression,
    is SolOrExpression,
    is SolEqExpression,
    is SolCompExpression -> SolBoolean
    is SolTernaryExpression -> inferExprType(expr.expressionList[1])
    is SolIndexAccessExpression -> {
      when (val arrType = inferExprType(expr.expressionList[0])) {
        is SolArray -> arrType.type
        is SolMapping -> arrType.to
        else -> SolUnknown
      }
    }
    is SolMemberAccessExpression -> {
      return SolResolver.resolveMemberAccess(expr)
        .firstOrNull()
        ?.parseType()
        ?: SolUnknown
    }
    is SolSeqExpression -> when {
      expr.expressionList.isEmpty() -> SolUnknown
      else -> inferExprType(expr.expressionList[0])
    }
    is SolUnaryExpression ->
      inferExprType(expr.expression)
    else -> SolUnknown
  }
}

private fun getNumericExpressionType(firstType: SolType, secondType: SolType): SolType {
  return if (firstType is SolInteger && secondType is SolInteger) {
    SolInteger(!(!firstType.unsigned || !secondType.unsigned), max(firstType.size, secondType.size))
  } else {
    SolUnknown
  }
}

fun SolExpression.getMembers(): List<SolMember> {
  return when {
    this is SolPrimaryExpression && varLiteral?.name == "super" -> {
      val contract = this.findContract()
      contract?.let { SolResolver.resolveContractMembers(it, true) }
        ?: emptyList()
    }
    else -> {
      this.type.getMembers(this.project)
    }
  }
}

val SolExpression.type: SolType
  get() {
    if (!isValid) {
      return SolUnknown
    }
    return RecursionManager.doPreventingRecursion(this, true) {
      CachedValuesManager.getCachedValue(this) {
        CachedValueProvider.Result.create(inferExprType(this), PsiModificationTracker.MODIFICATION_COUNT)
      }
    } ?: SolUnknown
  }

