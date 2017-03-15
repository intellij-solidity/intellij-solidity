package me.serce.solidity.lang.resolve.ref

import me.serce.solidity.lang.completion.SolCompleter
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.resolve.SolResolver

class SolUserDefinedTypeNameReference(element: SolUserDefinedTypeName) : SolReferenceBase<SolUserDefinedTypeName>(element), SolReference {
  override fun multiResolve() = SolResolver.resolveTypeName(element)

  override fun getVariants() = SolCompleter.completeTypeName(element)
}
