package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.firstInstance
import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.FunctionResolveResult
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.SolInternalTypeFactory
import me.serce.solidity.lang.types.findContract
import me.serce.solidity.lang.types.type

class SolUserDefinedTypeNameReference(element: SolUserDefinedTypeName) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveTypeNameUsingImports(element)

  override fun getVariants() = SolCompleter.completeTypeName(element)
}

class SolVarLiteralReference(element: SolVarLiteral) : SolReferenceBase<SolVarLiteral>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveVarLiteral(element)

  override fun getVariants() = SolCompleter.completeLiteral(element)
}

class SolModifierReference(element: SolFunctionDefinition, val modifierElement: PsiElement) : SolReferenceBase<SolFunctionDefinition>(element), SolReference {
  override fun calculateDefaultRangeInElement() = modifierElement.parentRelativeRange

  override fun multiResolve(): List<SolNamedElement> {
    val contract = element.ancestors.firstInstance<SolContractDefinition>()
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

  fun resolveFunctionCall(): Collection<FunctionResolveResult> {
    val (base, refName) = element.getBaseAndReferenceNameElement()
    return when {
      base == null -> {
        val globalType = SolInternalTypeFactory.of(element.project).globalType.ref
        val global = SolResolver.resolveFunction(SolContract(globalType), refName, element.functionCallArguments)

        val casts = SolResolver.resolveCast(refName, element.functionCallArguments)

        val contract = findContract(element)
        val regular = contract?.let { SolResolver.resolveFunction(SolContract(it), refName, element.functionCallArguments) } ?: emptyList()

        global + casts + regular
      }
      base is SolPrimaryExpression && base.varLiteral?.name == "super" -> {
        val contract = findContract(base)
        contract?.let { SolResolver.resolveFunction(SolContract(it), refName, element.functionCallArguments, true) } ?: emptyList()
      }
      else -> {
        SolResolver.resolveFunction(base.type, refName, element.functionCallArguments)
      }
    }
  }

  override fun multiResolve(): Collection<PsiElement> {
    return resolveFunctionCall().map { it.psiElement }
  }
}
