package me.serce.solidity.lang.types

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
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
    is SolUserDefinedLocationTypeName -> getSolTypeFromUserDefinedTypeName(type.userDefinedTypeName!!)
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

fun inferRefType(ref: SolVarLiteral): SolType {
  return when {
    ref.name == "this" -> {
      findContract(ref)
        ?.let { SolContract(it) } ?: SolUnknown
    }
    ref.name == "super" -> SolUnknown
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

fun findContract(element: PsiElement): SolContractDefinition? = element.findParentOrNull()

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
      val reference = expr.reference
      if (reference is SolFunctionCallReference) {
        reference.multiResolve().firstOrNull().let {
          when (it) {
            is SolFunctionDefinition -> it.returnType
            else -> SolUnknown
          }
        }
      } else {
        SolUnknown
      }
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

private fun getNumericExpressionType(firstType: SolType, secondType: SolType): SolType {
  return if (firstType is SolInteger && secondType is SolInteger) {
    SolInteger(!(!firstType.unsigned || !secondType.unsigned), max(firstType.size, secondType.size))
  } else {
    SolUnknown
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

val SolFunctionDefinition.returnType: SolType
  get() {
    return this.returns.let { list ->
      when (list) {
        null -> SolUnknown
        else -> list.parameterDefList.let {
          when {
            it.size == 1 -> getSolType(it[0].typeName)
            else -> SolTuple(it.map { def -> getSolType(def.typeName) })
          }
        }
      }
    }
  }

