package me.serce.solidity.lang.resolve.ref

import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.psi.SolidityNamedElement
import me.serce.solidity.lang.psi.SolidityUserDefinedTypeName
import me.serce.solidity.lang.stubs.SolidityGotoClassIndex

class SolidityUserDefinedTypeNameReferenceImpl(element: SolidityUserDefinedTypeName)
  : SolidityReferenceBase<SolidityUserDefinedTypeName>(element), SolidityReference {
  override fun multiResolve() = StubIndex.getElements(
    SolidityGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolidityNamedElement::class.java
  ).toList()
}
