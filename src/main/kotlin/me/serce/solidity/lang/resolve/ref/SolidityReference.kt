package me.serce.solidity.lang.resolve.ref

import com.intellij.psi.PsiPolyVariantReference
import me.serce.solidity.lang.psi.SolidityElement

interface SolidityReference : PsiPolyVariantReference {
  override fun getElement(): SolidityElement

  override fun resolve(): SolidityElement?
}
