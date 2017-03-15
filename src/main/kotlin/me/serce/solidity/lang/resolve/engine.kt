package me.serce.solidity.lang.resolve

import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.stubs.SolGotoClassIndex

object SolResolver {
  fun resolveTypeName(element: SolUserDefinedTypeName) = StubIndex.getElements(
    SolGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolNamedElement::class.java
  ).toList()

}
