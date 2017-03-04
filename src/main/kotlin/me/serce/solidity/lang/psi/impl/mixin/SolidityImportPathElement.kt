package me.serce.solidity.lang.psi.impl.mixin

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.core.SolidityTokenTypes.IDENTIFIER
import me.serce.solidity.lang.psi.SolidityContractDefinition
import me.serce.solidity.lang.psi.SolidityEnumDefinition
import me.serce.solidity.lang.psi.SolidityReferenceElement
import me.serce.solidity.lang.psi.SolidityUserDefinedTypeName
import me.serce.solidity.lang.psi.impl.SolidityNamedElementImpl
import me.serce.solidity.lang.psi.impl.SolidityStubbedElementImpl
import me.serce.solidity.lang.psi.impl.SolidityStubbedNamedElementImpl
import me.serce.solidity.lang.psi.impl.SolidityUserDefinedTypeNameImpl
import me.serce.solidity.lang.resolve.ref.SolidityImportPathReference
import me.serce.solidity.lang.resolve.ref.SolidityReference
import me.serce.solidity.lang.resolve.ref.SolidityUserDefinedTypeNameReferenceImpl
import me.serce.solidity.lang.stubs.SolidityContractOrLibDefStub
import me.serce.solidity.lang.stubs.SolidityEnumDefStub
import me.serce.solidity.lang.stubs.SolidityTypeRefStub

open class SolidityImportPathElement(node: ASTNode) : SolidityNamedElementImpl(node) {
  override fun getReference() = SolidityImportPathReference(this)
}

abstract class SolidityEnumItemImplMixin : SolidityStubbedNamedElementImpl<SolidityEnumDefStub>, SolidityEnumDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolidityEnumDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.ENUM
}

abstract class SolidityContractOrLibMixin : SolidityStubbedNamedElementImpl<SolidityContractOrLibDefStub>, SolidityContractDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolidityContractOrLibDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.CONTRACT
}


abstract class SolidityUserDefinedTypeNameImplMixin : SolidityStubbedElementImpl<SolidityTypeRefStub>, SolidityUserDefinedTypeName {
  constructor(node: ASTNode) : super(node)

  constructor(stub: SolidityTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolidityReference = SolidityUserDefinedTypeNameReferenceImpl(this)

  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String get() = (stub as? SolidityReferenceElement)?.referenceName ?: referenceNameElement.text

  override fun getParent(): PsiElement? = parentByStub

  override fun setName(name: String) = this
}
