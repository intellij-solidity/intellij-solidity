package me.serce.solidity.lang.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import me.serce.solidity.lang.resolve.ref.SolidityReference

interface SolidityElement : PsiElement {
  override fun getReference(): PsiReference?
}

interface SolidityNamedElement : SolidityElement, PsiNamedElement, NavigatablePsiElement


interface SolidityEnumDefElement : SolidityNamedElement

interface SolidityContractOrLibElement : SolidityNamedElement

interface SolidityReferenceElement: SolidityNamedElement {
  val referenceNameElement: PsiElement
  val referenceName: String

  override fun getReference(): SolidityReference
}
