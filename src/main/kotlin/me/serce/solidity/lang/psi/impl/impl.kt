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
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolPsiFactory
import me.serce.solidity.lang.resolve.ref.SolReference
import me.serce.solidity.lang.stubs.SolNamedStub

abstract class SolElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), SolElement {
  override fun getReference(): SolReference? = null
}

abstract class SolNamedElementImpl(node: ASTNode) : SolElementImpl(node), SolNamedElement, PsiNameIdentifierOwner {
  override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

  override fun getName(): String? {
    return nameIdentifier?.text
  }

  override fun setName(name: String): PsiElement? {
    nameIdentifier?.replace(SolPsiFactory(project).createIdentifier(name))
    return this
  }

  override fun getNavigationElement(): PsiElement = nameIdentifier ?: this

  override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()
}


abstract class SolStubbedElementImpl<StubT : StubElement<*>> : StubBasedPsiElementBase<StubT>, SolElement {

  constructor(node: ASTNode) : super(node)

  constructor(stub: StubT, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolReference? = null

  // FQN isn't needed in paring tests
  override fun toString(): String = "${javaClass.simpleName}($elementType)"
}


abstract class SolStubbedNamedElementImpl<S> :
  SolStubbedElementImpl<S>,
  SolNamedElement,
  PsiNameIdentifierOwner where S : SolNamedStub, S : StubElement<*> {

  constructor(node: ASTNode) : super(node)

  constructor(stub: S, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getNameIdentifier(): PsiElement? = findChildByType(IDENTIFIER)

  override fun getName() = stub?.name ?: nameIdentifier?.text

  override fun setName(name: String): PsiElement? {
    nameIdentifier?.replace(SolPsiFactory(project).createIdentifier(name))
    return this
  }

  override fun getNavigationElement(): PsiElement = nameIdentifier ?: this

  override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

  override fun getPresentation() = PresentationData(name, "", getIcon(0), null)
}
