package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import me.serce.solidity.lang.psi.SolidityNamedElement

abstract class SolidityReferenceBase<T: SolidityNamedElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), SolidityReference {
  override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)

  override fun getVariants(): Array<out Any> = emptyArray()

  override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
    return singleResolve()?.let { arrayOf(PsiElementResolveResult(it)) } ?: emptyArray()
  }

  open fun singleResolve(): PsiElement? = null
}
