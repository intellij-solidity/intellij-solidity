package me.serce.solidity.lang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import me.serce.solidity.lang.core.SolidityTokenTypes.IDENTIFIER
import me.serce.solidity.lang.psi.SolidityElement
import me.serce.solidity.lang.psi.SolidityNamedElement
import me.serce.solidity.lang.resolve.ref.SolidityReference

abstract class SolidityElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), SolidityElement {
  override fun getReference(): SolidityReference? = null
}

abstract class SolidityNamedElementImpl(node: ASTNode) : SolidityElementImpl(node), SolidityNamedElement, PsiNameIdentifierOwner {
  override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

  override fun getName(): String? = nameIdentifier?.text

  override fun setName(name: String): PsiElement? {
    return this
  }

  override fun getNavigationElement(): PsiElement = nameIdentifier ?: this

  override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}
