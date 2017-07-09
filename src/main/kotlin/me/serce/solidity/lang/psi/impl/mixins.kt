package me.serce.solidity.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstInstance
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.ref.*
import me.serce.solidity.lang.stubs.*
import me.serce.solidity.lang.types.SolUnknown
import me.serce.solidity.lang.types.inferExprType
import me.serce.solidity.lang.types.type
import java.util.*

open class SolImportPathElement(node: ASTNode) : SolNamedElementImpl(node), SolReferenceElement {
  override val referenceNameElement: PsiElement
    get() = findChildByType(STRINGLITERAL)!!
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolImportPathReference(this)
}

abstract class SolEnumItemImplMixin : SolStubbedNamedElementImpl<SolEnumDefStub>, SolEnumDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolEnumDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.ENUM
}

abstract class SolContractOrLibMixin : SolStubbedNamedElementImpl<SolContractOrLibDefStub>, SolContractDefinition {
  override val supers: List<SolUserDefinedTypeName>
    get() = findChildrenByType<SolInheritanceSpecifier>(INHERITANCE_SPECIFIER)
      .map { it.children.filterIsInstance(SolUserDefinedTypeName::class.java).firstOrNull() }
      .filterNotNull()
  override val collectSupers: Collection<SolUserDefinedTypeName>
    get() = CachedValuesManager.getCachedValue(this) {
      val collectedSupers = RecursionManager.doPreventingRecursion(this, true) {
        val collectedSupers = LinkedHashSet<SolUserDefinedTypeName>()
        val deque: Deque<SolUserDefinedTypeName> = ArrayDeque()
        deque.addAll(supers)
        while (deque.isNotEmpty()) {
          val sup: SolUserDefinedTypeName = deque.poll()
          collectedSupers.add(sup)
          val typeNames = SolResolver.resolveTypeName(sup).filterIsInstance<SolUserDefinedTypeName>()
          deque.addAll(typeNames)
        }

        collectedSupers
      }
      CachedValueProvider.Result.create(collectedSupers, PsiModificationTracker.MODIFICATION_COUNT)
    }

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolContractOrLibDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.CONTRACT
}

abstract class SolFunctionDefMixin : SolStubbedNamedElementImpl<SolFunctionDefStub>, SolFunctionDefinition {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!
  override val referenceName: String
    get() = referenceNameElement.text
  override val modifiers: List<PsiElement>
    get() = findChildrenByType<PsiElement>(FUNCTION_MODIFIER)
  override val parameters: List<SolParameterDef>
    get() = findChildByType<SolParameterList>(PARAMETER_LIST)!!
      .children
      .filterIsInstance(SolParameterDef::class.java)
  override val contract: SolContractDefinition
    get() = this.ancestors.asSequence()
      .filterIsInstance<SolContractDefinition>()
      .first()

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolFunctionDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference() = references.firstOrNull()

  override fun getReferences(): Array<SolReference> {
    return modifiers.map { SolModifierReference(this, it) }.toTypedArray()
  }

  override fun getIcon(flags: Int) = SolidityIcons.FUNCTION
}

abstract class SolModifierDefMixin : SolStubbedNamedElementImpl<SolModifierDefStub>, SolModifierDefinition {
  override val contract: SolContractDefinition
    get() = ancestors.firstInstance<SolContractDefinition>()

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolModifierDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.FUNCTION
}


abstract class SolStateVarDeclMixin : SolStubbedNamedElementImpl<SolStateVarDeclStub>, SolStateVariableDeclaration {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStateVarDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.STATE_VAR
}

abstract class SolStructDefMixin : SolStubbedNamedElementImpl<SolStructDefStub>, SolStructDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStructDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.STRUCT
}


abstract class SolVarLiteralMixin(node: ASTNode) : SolNamedElementImpl(node), SolVarLiteral {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference(): SolReference = SolVarLiteralReference(this)
}

open class SolVariableDeclarationMixin(node: ASTNode) : SolNamedElementImpl(node)

open class SolParameterDefMixin(node: ASTNode) : SolNamedElementImpl(node)

abstract class SolUserDefinedTypeNameImplMixin : SolStubbedElementImpl<SolTypeRefStub>, SolUserDefinedTypeName {
  constructor(node: ASTNode) : super(node)

  constructor(stub: SolTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolReference = SolUserDefinedTypeNameReference(this)

  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String
    get() = referenceNameElement.text

  override fun getParent(): PsiElement? = parentByStub

  override fun getName(): String? {
    return referenceNameElement.text
  }

  override fun setName(name: String): SolUserDefinedTypeNameImplMixin {
    referenceNameElement.replace(SolPsiFactory(project).createIdentifier(name))
    return this
  }
}

abstract class SolMemberAccessElement(node: ASTNode) : SolNamedElementImpl(node), SolMemberAccessExpression {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolMemberAccessReference(this)
}

abstract class SolFunctionCallElement(node: ASTNode) : SolNamedElementImpl(node), SolFunctionCallExpression {
  // TODO: simplify
  override val referenceNameElement: PsiElement
    get() = findLastChildByType(IDENTIFIER) ?: firstChild
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolFunctionCallReference(this)
}

abstract class SolEventDefMixin(node: ASTNode) : SolNamedElementImpl(node), SolEventDefinition
