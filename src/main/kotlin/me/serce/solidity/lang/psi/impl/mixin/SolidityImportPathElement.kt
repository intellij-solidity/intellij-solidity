package me.serce.solidity.lang.psi.impl.mixin

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.psi.SolidityContractDefinition
import me.serce.solidity.lang.psi.SolidityEnumDefinition
import me.serce.solidity.lang.psi.impl.SolidityNamedElementImpl
import me.serce.solidity.lang.psi.impl.SolidityStubbedNamedElementImpl
import me.serce.solidity.lang.resolve.ref.SolidityPathReference
import me.serce.solidity.lang.stubs.SolidityContractOrLibDefStub
import me.serce.solidity.lang.stubs.SolidityEnumDefStub

open class SolidityImportPathElement(node: ASTNode) : SolidityNamedElementImpl(node) {
  override fun getReference() = SolidityPathReference(this)
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

