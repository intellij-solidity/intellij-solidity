package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import me.serce.solidity.lang.psi.SolidityElement
import me.serce.solidity.lang.psi.SolidityNamedElement

interface SolidityReference : PsiPolyVariantReference {
  override fun getElement(): SolidityElement

  override fun resolve(): SolidityElement?
}

abstract class SolidityReferenceBase<T: SolidityNamedElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), SolidityReference {
  override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)

  override fun getVariants(): Array<out Any> = emptyArray()

  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    return singleResolve()?.let { arrayOf(PsiElementResolveResult(it)) } ?: emptyArray()
  }

  open fun singleResolve(): PsiElement? = null
}
