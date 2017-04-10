package me.serce.solidity.lang.resolve.ref

import com.intellij.psi.PsiElement
import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.psi.SolVarLiteral
import me.serce.solidity.lang.psi.impl.SolVarLiteralMixin
import me.serce.solidity.lang.psi.parentRelativeRange
import me.serce.solidity.lang.resolve.SolResolver

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

  override fun multiResolve() = SolResolver.resolveModifier(modifierElement)

  override fun getVariants() = SolCompleter.completeModifier(modifierElement)
}
