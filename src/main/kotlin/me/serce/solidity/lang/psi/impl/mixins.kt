package me.serce.solidity.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.stubs.IStubElementType
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.core.SolidityTokenTypes.FUNCTION_MODIFIER
import me.serce.solidity.lang.core.SolidityTokenTypes.IDENTIFIER
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.ref.SolUserDefinedTypeNameReference
import me.serce.solidity.lang.resolve.ref.SolImportPathReference
import me.serce.solidity.lang.resolve.ref.SolModifierReference
import me.serce.solidity.lang.resolve.ref.SolReference
import me.serce.solidity.lang.stubs.*

open class SolImportPathElement(node: ASTNode) : SolNamedElementImpl(node) {
  override fun getReference() = SolImportPathReference(this)
}

abstract class SolEnumItemImplMixin : SolStubbedNamedElementImpl<SolEnumDefStub>, SolEnumDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolEnumDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.ENUM
}

abstract class SolContractOrLibMixin : SolStubbedNamedElementImpl<SolContractOrLibDefStub>, SolContractDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolContractOrLibDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.CONTRACT
}

abstract class SolFunctionDefMixin : SolStubbedNamedElementImpl<SolFunctionDefStub>, SolFunctionDefinition {
  override val modifiers: List<PsiElement>
    get() {
      return findChildrenByType<PsiElement>(FUNCTION_MODIFIER)
    }

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolFunctionDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference() = references.firstOrNull()

  override fun getReferences(): Array<SolReference> {
    return modifiers.map { SolModifierReference(this, it) }.toTypedArray()
  }

  override fun getIcon(flags: Int) = SolidityIcons.FUNCTION
}

abstract class SolModifierDefMixin : SolStubbedNamedElementImpl<SolModifierDefStub>, SolModifierDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolModifierDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.FUNCTION
}


abstract class SolStateVarDeclMixin : SolStubbedNamedElementImpl<SolStateVarDeclStub>, SolStateVariableDeclaration {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStateVarDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.STATE_VAR
}


abstract class SolUserDefinedTypeNameImplMixin : SolStubbedElementImpl<SolTypeRefStub>, SolUserDefinedTypeName {
  constructor(node: ASTNode) : super(node)

  constructor(stub: SolTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolReference = SolUserDefinedTypeNameReference(this)

  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String get() = (stub as? SolReferenceElement)?.referenceName ?: referenceNameElement.text

  override fun getParent(): PsiElement? = parentByStub

  override fun setName(name: String) = this
}
