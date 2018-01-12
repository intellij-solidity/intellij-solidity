package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.firstInstance
import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolFunctionCallElement
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.type

class SolUserDefinedTypeNameReference(element: SolUserDefinedTypeName) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveTypeName(element)

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

class SolMemberAccessReference (element: SolMemberAccessExpression): SolReferenceBase<SolMemberAccessExpression>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.identifier?.parentRelativeRange ?: super.calculateDefaultRangeInElement()
  }

  override fun multiResolve() = SolResolver.resolveMemberAccess(element)

  override fun getVariants() = SolCompleter.completeMemberAccess(element)
}

class SolFunctionCallReference (element: SolFunctionCallElement): SolReferenceBase<SolFunctionCallElement>(element), SolReference {
  override fun calculateDefaultRangeInElement(): TextRange {
    return element.referenceNameElement.parentRelativeRange
  }

  override fun multiResolve(): List<PsiElement> {
    val contract: SolContractDefinition? = when {
      element.expressionList.isEmpty() -> element.ancestors.firstInstance<SolContractDefinition>()
      else -> (element.expressionList.first().type as? SolContract)?.ref
    }

    return when(contract) {
      null -> emptyList()
      else -> SolResolver.resolveFunction(contract, element)
    }
  }
}
