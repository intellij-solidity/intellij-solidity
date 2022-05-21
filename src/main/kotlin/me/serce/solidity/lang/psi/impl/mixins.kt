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

abstract class SolUserDefinedValueTypeDefMixin : SolStubbedNamedElementImpl<SolUserDefinedValueTypeDefStub>,
  SolUserDefinedValueTypeDefinition {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolUserDefinedValueTypeDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
}

abstract class SolEnumValueMixin(node: ASTNode) : SolNamedElementImpl(node), SolEnumValue {
  override val contract: SolContractDefinition
    get() = ancestors.firstInstance()

  override fun resolveElement(): SolNamedElement? = this

  override fun parseType(): SolType {
    val def = parentOfType<SolEnumDefinition>()
    return def?.let { SolEnum(it) } ?: SolUnknown
  }

  override fun getPossibleUsage(contextType: ContextType) = Usage.VARIABLE
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

  override fun parseType(): SolType {
    return SolContract(this)
  }

  override fun resolveElement() = this
  override val callablePriority = 1000
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

  override val callablePriority = 0

  override fun parseType(): SolType {
    return this.returns.let { list ->
      when (list) {
        null -> SolUnknown
        else -> list.parameterDefList.let {
          when (it.size) {
            1 -> getSolType(it[0].typeName)
            else -> SolTuple(it.map { def -> getSolType(def.typeName) })
          }
        }
      }
    }
  }

  override val visibility
    get() = functionVisibilitySpecifierList
      .map { it.text.uppercase() }
      .mapNotNull { safeValueOf<Visibility>(it) }
      .firstOrNull()
      ?: Visibility.PUBLIC

  override fun getPossibleUsage(contextType: ContextType) =
    if (isPossibleToUse(contextType))
      Usage.CALLABLE
    else
      null

  private fun isPossibleToUse(contextType: ContextType): Boolean {
    val visibility = this.visibility
    return visibility != Visibility.PRIVATE
      && !(visibility == Visibility.EXTERNAL && contextType == ContextType.SUPER)
      && !(visibility == Visibility.INTERNAL && contextType == ContextType.EXTERNAL)
  }

  override fun resolveElement() = this

  override val returns: SolParameterList?
    get() = if (parameterListList.size == 2) {
      parameterListList[1]
    } else {
      null
    }

  override val contract: SolContractDefinition?
    get() = this.ancestors.asSequence()
      .filterIsInstance<SolContractDefinition>()
      .firstOrNull()

  override val isConstructor: Boolean
    get() = contract?.name == name

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

  override fun parseParameters(): List<Pair<String?, SolType>> = emptyList()

  override fun parseType(): SolType = getSolType(typeName)

  override fun getPossibleUsage(contextType: ContextType): Usage? {
    val visibility = this.visibility
    return if (contextType == ContextType.SUPER)
      Usage.VARIABLE
    else if (contextType == ContextType.EXTERNAL && visibility == Visibility.PUBLIC)
      Usage.CALLABLE
    else
      null
  }

  override val callablePriority = 0

  override fun resolveElement() = this

  override val visibility
    get() = visibilityModifier?.text?.let { safeValueOf<Visibility>(it.uppercase()) } ?: Visibility.INTERNAL
}

abstract class SolConstantVariableDeclMixin : SolStubbedNamedElementImpl<SolConstantVariableDeclStub>, SolConstantVariableDeclaration {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolConstantVariableDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  // TODO: does it need a separate icon?
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

  override fun parseType(): SolType {
    return SolStruct(this)
  }

  override fun resolveElement() = this

  override val callablePriority = 1000
}

abstract class SolFunctionCallMixin(node: ASTNode) : SolNamedElementImpl(node), SolFunctionCallElement, SolFunctionCallExpression {

  private fun getReferenceNameElement(expr: SolExpression): PsiElement {
    return when (expr) {
      is SolPrimaryExpression ->
        expr.varLiteral ?: expr.elementaryTypeName!!
      is SolMemberAccessExpression ->
        expr.identifier!!
      is SolFunctionCallExpression ->
        expr.firstChild
      is SolIndexAccessExpression ->
        expr.firstChild
      is SolNewExpression ->
        expr.typeName as PsiElement
      is SolSeqExpression ->
        getReferenceNameElement(expr.expressionList.first())
      // unable to extract reference name element
      else -> expr
    }
  }

  override val expression: SolExpression
    get() = expressionList.first()

  override val referenceNameElement: PsiElement
    get() = getReferenceNameElement(expression)

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

  override fun getReference(): SolReference = SolModifierReference(this, this)
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

abstract class SolVariableDeclarationMixin(node: ASTNode) : SolVariableDeclaration, SolNamedElementImpl(node)

open class SolParameterDefMixin(node: ASTNode) : SolNamedElementImpl(node)

abstract class SolUserDefinedTypeNameImplMixin : SolStubbedElementImpl<SolTypeRefStub>, SolUserDefinedTypeName {
  constructor(node: ASTNode) : super(node)

  constructor(stub: SolTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolReference = SolUserDefinedTypeNameReference(this)

  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override fun findIdentifiers(): List<PsiElement> =
    findChildrenByType(IDENTIFIER)

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
    get() = typeName ?: firstChild
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

  override fun parseType(): SolType {
    return SolUnknown
  }

  override fun resolveElement() = this

  override val callablePriority = 1000
}

abstract class SolErrorDefMixin : SolStubbedNamedElementImpl<SolErrorDefStub>, SolErrorDefinition, SolCallableElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolErrorDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  //todo add error args identifiers
  override fun parseParameters(): List<Pair<String?, SolType>> {
    return indexedParameterList?.typeNameList
      ?.map { null to getSolType(it) }
      ?: emptyList()
  }

  override fun getNameIdentifier(): PsiElement? {
    // use the second identifier because "error" isn't a keyword but also an identifier
    return findChildrenByType<PsiElement>(IDENTIFIER).getOrNull(1)
  }

  override fun parseType(): SolType {
    return SolUnknown
  }

  override fun resolveElement() = this

  override val callablePriority = 1000
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
  override val library: SolContractDefinition?
    get() = SolResolver.resolveTypeNameUsingImports(getTypeNameList()[0] as SolUserDefinedTypeName)
      .filterIsInstance<SolContractDefinition>()
      .firstOrNull()
}
