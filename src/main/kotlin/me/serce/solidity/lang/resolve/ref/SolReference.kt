package me.serce.solidity.lang.resolve.ref

import com.intellij.psi.PsiPolyVariantReference
import me.serce.solidity.lang.psi.SolElement

interface SolReference : PsiPolyVariantReference {
  override fun getElement(): SolElement

  override fun resolve(): SolElement?
}
