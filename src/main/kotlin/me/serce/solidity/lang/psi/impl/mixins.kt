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
import me.serce.solidity.lang.types.*
import java.util.*
import javax.naming.OperationNotSupportedException

open class SolImportPathElement : SolStubbedNamedElementImpl<SolImportPathDefStub>, SolReferenceElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolImportPathDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override val referenceNameElement: PsiElement
    get() = findChildByType(STRINGLITERAL)!!
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolImportPathReference(this)
}

open class SolImportAliasMixin(node: ASTNode) : SolNamedElementImpl(node)

abstract class SolEnumItemImplMixin : SolStubbedNamedElementImpl<SolEnumDefStub>, SolEnumDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolEnumDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override val contract: SolContractDefinition
    get() = ancestors.firstInstance()

  override fun getIcon(flags: Int) = SolidityIcons.ENUM
}

abstract class SolEnumValueMixin(node: ASTNode) : SolNamedElementImpl(node), SolEnumValue {
  override val contract: SolContractDefinition
    get() = ancestors.firstInstance()
}

abstract class SolContractOrLibMixin : SolStubbedNamedElementImpl<SolContractOrLibDefStub>, SolContractDefinition {
  override val supers: List<SolUserDefinedTypeName>
    get() = inheritanceSpecifierList
      .mapNotNull { it.userDefinedTypeName }

  override val collectSupers: Collection<SolUserDefinedTypeName>
    get() = RecursionManager.doPreventingRecursion(this, true) {
      CachedValuesManager.getCachedValue(this) {
        val collectedSupers = LinkedHashSet<SolUserDefinedTypeName>()
        collectedSupers.addAll(supers)
        collectedSupers.addAll(
          supers.mapNotNull { it.reference?.resolve() }
            .filterIsInstance<SolContractOrLibElement>()
            .flatMap { it.collectSupers }
        )
        CachedValueProvider.Result.create(collectedSupers, PsiModificationTracker.MODIFICATION_COUNT)
      }
    } ?: emptyList()

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolContractOrLibDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.CONTRACT

  override fun parseParameters(): List<Pair<String?, SolType>> {
    return listOf(Pair(null, SolAddress))
  }

  override fun parseReturnType(): SolType {
    return SolContract(this)
  }

  override val resolvedElement: SolNamedElement
    get() = this
}

abstract class SolConstructorDefMixin(node: ASTNode) : SolElementImpl(node), SolConstructorDefinition {
  override val referenceNameElement: PsiElement
    get() = this

  override val referenceName: String
    get() = "constructor"

  override fun setName(name: String): PsiElement {
    throw OperationNotSupportedException("constructors don't have name")
  }

  override fun getReference(): SolReference? = references.firstOrNull()

  override fun getReferences(): Array<SolReference> {
    return findChildrenByType<SolModifierInvocation>(MODIFIER_INVOCATION)
      .map { SolModifierReference(this, it) }.toTypedArray()
  }
}

abstract class SolFunctionDefMixin : SolStubbedNamedElementImpl<SolFunctionDefStub>, SolFunctionDefinition {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String
    get() = referenceNameElement.text

  override val modifiers: List<SolModifierInvocation>
    get() = findChildrenByType(MODIFIER_INVOCATION)

  override val parameters: List<SolParameterDef>
    get() = findChildByType<SolParameterList>(PARAMETER_LIST)
      ?.children
      ?.filterIsInstance(SolParameterDef::class.java)
      ?: emptyList()

  override fun parseParameters(): List<Pair<String?, SolType>> {
    return parameters.map { it.identifier?.text to getSolType(it.typeName) }
  }

  override fun parseReturnType(): SolType {
    return this.returns.let { list ->
      when (list) {
        null -> SolUnknown
        else -> list.parameterDefList.let {
          when {
            it.size == 1 -> getSolType(it[0].typeName)
            else -> SolTuple(it.map { def -> getSolType(def.typeName) })
          }
        }
      }
    }
  }

  override val resolvedElement: SolNamedElement
    get() = this

  override val returns: SolParameterList?
    get() = if (parameterListList.size == 2) {
      parameterListList[1]
    } else {
      null
    }

  override val contract: SolContractDefinition
    get() = this.ancestors.asSequence()
      .filterIsInstance<SolContractDefinition>()
      .first()

  override val isConstructor: Boolean
    get() = contract.name == name

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
    get() = ancestors.firstInstance()

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolModifierDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.FUNCTION
}

abstract class SolStateVarDeclMixin : SolStubbedNamedElementImpl<SolStateVarDeclStub>, SolStateVariableDeclaration {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStateVarDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.STATE_VAR
}

abstract class SolStructDefMixin : SolStubbedNamedElementImpl<SolStructDefStub>, SolStructDefinition, SolCallableElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStructDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.STRUCT

  override fun parseParameters(): List<Pair<String?, SolType>> {
    return variableDeclarationList
      .map { it.identifier?.text to getSolType(it.typeName) }


  }

  override fun parseReturnType(): SolType {
    return SolStruct(this)
  }

  override val resolvedElement: SolNamedElement
    get() = this
}

abstract class SolFunctionCallMixin(node: ASTNode) : SolNamedElementImpl(node), SolFunctionCallElement {
  override fun getBaseAndReferenceNameElement(): Pair<SolExpression?, PsiElement> {
    return when (val expr = expression) {
      is SolPrimaryExpression ->
        expr.varLiteral?.let { Pair(null, it) }
          ?: expr.elementaryTypeName?.let { Pair(null, it) }!!
      is SolMemberAccessExpression ->
        Pair(expr.expression, expr.identifier!!)
      else -> throw IllegalStateException("unable to extract reference name element from $this")
    }
  }

  override val referenceNameElement: PsiElement
    get() = getBaseAndReferenceNameElement().second

  override val referenceName: String
    get() = referenceNameElement.text

  override fun getName(): String? = referenceName

  override fun getReference(): SolReference = SolFunctionCallReference(this as SolFunctionCallExpression)
}

abstract class SolModifierInvocationMixin(node: ASTNode) : SolNamedElementImpl(node), SolModifierInvocationElement {

  override val referenceNameElement: PsiElement
    get() = this.varLiteral
  override val referenceName: String
    get() = this.varLiteral.text

  override fun getReference(): SolReference = SolModifierReference(this.findParent<SolHasModifiersElement>(), this)
}

abstract class SolVarLiteralMixin(node: ASTNode) : SolNamedElementImpl(node), SolVarLiteral {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference(): SolReference = SolVarLiteralReference(this)
}

open class SolDeclarationItemMixin(node: ASTNode) : SolNamedElementImpl(node)

open class SolTypedDeclarationItemMixin(node: ASTNode) : SolNamedElementImpl(node)

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

abstract class SolNewExpressionElement(node: ASTNode) : SolNamedElementImpl(node), SolNewExpression {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER) ?: firstChild
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolNewExpressionReference(this)
}

abstract class SolEventDefMixin : SolStubbedNamedElementImpl<SolEventDefStub>, SolEventDefinition, SolCallableElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolEventDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  //todo add event args identifiers
  override fun parseParameters(): List<Pair<String?, SolType>> {
    return indexedParameterList?.typeNameList
      ?.map { null to getSolType(it) }
      ?: emptyList()
  }

  override fun parseReturnType(): SolType {
    return SolUnknown
  }

  override val resolvedElement: SolNamedElement
    get() = this
}

abstract class SolUsingForMixin(node: ASTNode) : SolElementImpl(node), SolUsingForElement {
  override val type: SolType?
    get() {
      val list = getTypeNameList()
      return if (list.size > 1) {
        getSolType(list[1])
      } else {
        null
      }
    }
  override val library: SolContractDefinition
    get() = SolResolver.resolveTypeNameUsingImports(getTypeNameList()[0] as SolUserDefinedTypeName)
      .filterIsInstance<SolContractDefinition>()
      .first()
}
