package me.serce.solidity.lang.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex

object SolResolver {
  fun resolveTypeName(element: SolUserDefinedTypeName): List<SolNamedElement> = StubIndex.getElements(
    SolGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolNamedElement::class.java
  ).toList()

  fun resolveModifier(modifier: PsiElement): List<SolNamedElement> = StubIndex.getElements(
    SolModifierIndex.KEY,
    modifier.text,
    modifier.project,
    null,
    SolNamedElement::class.java
  ).toList()
}
