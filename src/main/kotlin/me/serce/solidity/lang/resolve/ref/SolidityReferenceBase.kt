package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import me.serce.solidity.lang.psi.SolidityNamedElement

abstract class SolidityReferenceBase<T : SolidityNamedElement>(element: T) : PsiPolyVariantReferenceBase<T>(element), SolidityReference {
  override fun calculateDefaultRangeInElement() = TextRange(0, element.textRange.length)

  override fun getVariants(): Array<out Any> = emptyArray()

  override final fun multiResolve(incompleteCode: Boolean) = ResolveCache.getInstance(element.project)
    .resolveWithCaching(this, { r, incomplete ->
      r.multiResolve().map(::PsiElementResolveResult).toTypedArray()
    }, true, false)

  open fun multiResolve(): List<PsiElement> = singleResolve()?.let { listOf(it) } ?: emptyList()

  open fun singleResolve(): PsiElement? = null
}
