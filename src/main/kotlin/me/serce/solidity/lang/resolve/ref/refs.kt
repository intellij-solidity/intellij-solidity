package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.types.*
import me.serce.solidity.wrap

class SolUserDefinedTypeNameReference(element: SolUserDefinedTypeName) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveTypeNameUsingImports(element)

  override fun getVariants() = SolCompleter.completeTypeName(element)
}

class SolVarLiteralReference(element: SolVarLiteral) : SolReferenceBase<SolVarLiteral>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveVarLiteral(element)

  override fun getVariants() = SolCompleter.completeLiteral(element)
}

class SolModifierReference(element: SolReferenceElement, private val modifierElement: SolModifierInvocationElement) : SolReferenceBase<SolReferenceElement>(element), SolReference {
  override fun calculateDefaultRangeInElement() = modifierElement.parentRelativeRange

  override fun multiResolve(): List<SolNamedElement> {
    val contract = modifierElement.findContract()!!
    val superNames: List<String> = (contract.collectSupers.map { it.name } + contract.name).filterNotNull()
    return SolResolver.resolveModifier(modifierElement)
      .filter { it.contract.name in superNames }
  }

  override fun getVariants() = SolCompleter.completeModifier(modifierElement)
}

class SolMemberAccessReference(element: SolMemberAccessExpression) : SolReferenceBase<SolMemberAccessExpression>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.identifier?.parentRelativeRange ?: super.calculateDefaultRangeInElement()
  }

  override fun multiResolve() = SolResolver.resolveMemberAccess(element)

  override fun getVariants() = SolCompleter.completeMemberAccess(element)
}

class SolNewExpressionReference(element: SolNewExpression) : SolReferenceBase<SolNewExpression>(element), SolReference {

  override fun calculateDefaultRangeInElement(): TextRange {
    return element.referenceNameElement.parentRelativeRange
  }

  override fun multiResolve(): Collection<PsiElement> {
    val types = SolResolver.resolveTypeNameUsingImports(element)
    return types
      .filterIsInstance(SolContractDefinition::class.java)
      .flatMap {
        val constructors = it.findConstructors()
        if (constructors.isEmpty()) {
          listOf(it)
        } else {
          constructors
        }
      }
  }
}

fun SolContractDefinition.findConstructors(): List<SolElement> {
  return if (this.constructorDefinitionList.isNotEmpty()) {
    this.constructorDefinitionList
  } else {
    this.functionDefinitionList
      .filter { it.name == this.name }
  }
}

class SolFunctionCallReference(element: SolFunctionCallExpression) : SolReferenceBase<SolFunctionCallExpression>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.referenceNameElement.parentRelativeRange
  }

  fun resolveFunctionCall(): Collection<ResolvedCallable> {
    return when (val expr = element.expression) {
      is SolPrimaryExpression -> {
        val regular = (expr.varLiteral?.reference?.multiResolve() ?: emptyList())
          .filterIsInstance<ResolvedCallable>()
        val casts = resolveElementaryTypeCasts(expr)
        regular + casts
      }
      is SolMemberAccessExpression -> {
        val members = SolResolver.resolveMembers(expr)
          .filterIsInstance<ResolvedCallable>()
        val fromLibraries = resolveFunctionCallUsingLibraries(expr)
        members + fromLibraries
      }
      else ->
        emptyList()
    }
  }

  private fun resolveElementaryTypeCasts(expr: SolPrimaryExpression): Collection<ResolvedCallable> {
    return expr.elementaryTypeName
      ?.let {
        val type = getSolType(it)
        object : ResolvedCallable {
          override val resolvedElement: SolNamedElement? = null
          override fun parseParameters(): List<Pair<String?, SolType>> = listOf(null to SolUnknown)

          override fun parseReturnType(): SolType = type
        }
      }
      .wrap()
  }

  private fun resolveFunctionCallUsingLibraries(expression: SolMemberAccessExpression): Collection<ResolvedCallable> {
    val name = expression.identifier?.text
    return if (name != null) {
      val type = expression.expression.type
      if (type != SolUnknown) {
        val contract = expression.findContract()
        val superContracts = contract
          ?.collectSupers
          ?.flatMap { SolResolver.resolveTypeNameUsingImports(it) }
          ?.filterIsInstance<SolContractDefinition>()
          ?: emptyList()
        return (superContracts + contract.wrap())
          .flatMap { it.usingForDeclarationList }
          .filter {
            val usingType = it.type
            usingType == null || usingType == type
          }
          .map { it.library }
          .distinct()
          .flatMap { it.functionDefinitionList }
          .filter { it.name == name }
          .filter {
            val firstParam = it.parameters.firstOrNull()
            if (firstParam == null) {
              false
            } else {
              getSolType(firstParam.typeName).isAssignableFrom(type)
            }
          }
          .map { it.toLibraryCallable() }
      } else {
        emptyList()
      }
    } else {
      emptyList()
    }
  }

  private fun SolFunctionDefinition.toLibraryCallable(): ResolvedCallable {
    return object : ResolvedCallable {
      override fun parseParameters(): List<Pair<String?, SolType>> = this@toLibraryCallable.parseParameters().drop(1)

      override fun parseReturnType(): SolType = this@toLibraryCallable.parseReturnType()

      override val resolvedElement: SolNamedElement
        get() = this@toLibraryCallable
    }
  }

  override fun multiResolve(): Collection<PsiElement> {
    return resolveFunctionCall()
      .filter { it.canBeApplied(element.functionCallArguments) }
      .mapNotNull { it.resolvedElement }
  }
}
