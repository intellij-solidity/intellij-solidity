package me.serce.solidity.lang.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import me.serce.solidity.lang.core.SolidityTokenTypes.IDENTIFIER
import me.serce.solidity.lang.psi.SolidityElement
import me.serce.solidity.lang.psi.SolidityNamedElement
import me.serce.solidity.lang.resolve.ref.SolidityReference
import me.serce.solidity.lang.stubs.SolidityNamedStub

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


abstract class SolidityStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, SolidityElement {

  constructor(node: ASTNode) : super(node)

  constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolidityReference? = null

  // FQN isn't needed in paring tests
  override fun toString(): String = "${javaClass.simpleName}($elementType)"
}


abstract class SolidityStubbedNamedElementImpl<S> :
  SolidityStubbedElementImpl<S>,
  SolidityNamedElement,
  PsiNameIdentifierOwner where S : SolidityNamedStub, S : StubElement<*> {

  constructor(node: ASTNode) : super(node)

  constructor(stub: S, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

  override fun getName() = stub?.name ?: nameIdentifier?.text

  override fun setName(name: String): PsiElement? {
    return this
  }

  override fun getNavigationElement(): PsiElement = nameIdentifier ?: this

  override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

  override fun getPresentation() = PresentationData(name, "", getIcon(0), null)
}
