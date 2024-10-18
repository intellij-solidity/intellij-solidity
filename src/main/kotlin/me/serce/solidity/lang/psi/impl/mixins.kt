package me.serce.solidity.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.nextLeaf
import com.intellij.ui.IconManager
import me.serce.solidity.firstInstance
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.ref.*
import me.serce.solidity.lang.stubs.*
import me.serce.solidity.lang.types.*
import me.serce.solidity.wrap
import javax.naming.OperationNotSupportedException
import javax.swing.Icon

open class SolImportPathElement : SolStubbedNamedElementImpl<SolImportPathDefStub>, SolReferenceElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolImportPathDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override val referenceNameElement: PsiElement
    get() = findChildByType(STRINGLITERAL)!!
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolImportPathReference(this)
}

open class SolImportAliasMixin : SolStubbedNamedElementImpl<SolImportAliasDefStub>, SolNamedElement {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolImportAliasDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

}

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
  /**
   * In solidity, the given bases are searched from right to left (left to right in Python) in a depth-first manner, so we need reversed order
   * @return list of super contracts in reversed order
   */
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

  override fun getIcon(flags: Int) = icon

  override fun parseParameters(): List<Pair<String?, SolType>> {
    return listOf(Pair(null, object : SolType {
      override fun isAssignableFrom(other: SolType): Boolean {
        return when (other) {
          is SolAddress -> true
          is SolContract -> SolContract(this@SolContractOrLibMixin).isAssignableFrom(other)
          else -> false
        }
      }

      override fun toString(): String = this@SolContractOrLibMixin.name ?: ""
    }))
  }

  override fun parseType(): SolType {
    return SolContract(this)
  }

  override fun resolveElement() = this
  override val callablePriority = 1000

  override val isAbstract: Boolean
    get() = firstChild?.elementType == ABSTRACT
  override val contractType: ContractType
    get() {
      val typeEl = (if (isAbstract) firstChild?.nextLeaf { it !is PsiWhiteSpace } else firstChild) ?: return ContractType.COMMON
      return when (typeEl.elementType) {
        LIBRARY -> ContractType.LIBRARY
        INTERFACE -> ContractType.INTERFACE
        else -> ContractType.COMMON
      }
    }

  override val isPayable: Boolean
    get() = this.functionDefinitionList.any { it.specialFunction?.let { it == SpecialFunctionType.RECEIVE || it == SpecialFunctionType.FALLBACK } ?: false }

  override val icon: Icon
    get() = when (contractType) {
      ContractType.LIBRARY -> SolidityIcons.LIBRARY
      ContractType.INTERFACE -> SolidityIcons.INTERFACE
      ContractType.COMMON -> SolidityIcons.CONTRACT
    }
}

interface SolConstructorOrFunctionDef {
  fun getBlock(): SolBlock?
}

abstract class SolConstructorDefMixin(node: ASTNode) : SolElementImpl(node), SolConstructorDefinition, SolFunctionDefElement, SolConstructorOrFunctionDef {
  override val referenceNameElement: PsiElement
    get() = this

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

  override val callablePriority = 100

  override fun parseType(): SolType {
    return contract?.let { SolContract(it) } ?: SolUnknown
  }

  override val visibility
    get() = functionVisibilitySpecifierList.mapNotNull { it.visibilitySpecifier }.parseVisibility()

  override val mutability
    get() = stateMutabilitySpecifierList.parseMutability()

  override fun getPossibleUsage(contextType: ContextType) = Usage.CALLABLE

  override fun resolveElement() = this

  override val returns: SolParameterList?
    get() = null

  override val contract: SolContractDefinition?
    get() = this.ancestors.asSequence()
      .filterIsInstance<SolContractDefinition>()
      .firstOrNull()

  override val isConstructor: Boolean
    get() = true

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

  override val specialFunction: SpecialFunctionType?
    get() = null

  override fun getIcon(flags: Int): Icon? = SolidityIcons.CONSTRUCTOR
}

abstract class SolFunctionDefMixin : SolStubbedNamedElementImpl<SolFunctionDefStub>, SolFunctionDefinition, SolConstructorOrFunctionDef {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!

  override val referenceName: String
    get() = referenceNameElement.text

  override val modifiers: List<SolModifierInvocation>
    get() = findChildrenByType(MODIFIER_INVOCATION)

  override val parameters: List<SolParameterDef>
    get() = findChildByType<SolParameterList>(PARAMETER_LIST)
      ?.children
      ?.filterIsInstance<SolParameterDef>()
      ?: emptyList()

  override fun parseParameters(): List<Pair<String?, SolType>> {
    return Companion.parseParameters(parameters)
  }

  override val callablePriority = 0

  override fun parseType(): SolType {
    return this.returns.parseType()
  }

  override val visibility
    get() = functionVisibilitySpecifierList.mapNotNull { it.visibilitySpecifier }.parseVisibility()

  override val mutability
    get() = stateMutabilitySpecifierList.parseMutability()

  override val specialFunction: SpecialFunctionType?
    get() = firstChild?.text?.uppercase().let { t -> SpecialFunctionType.values().find { it.name == t } }


  override fun getPossibleUsage(contextType: ContextType) =
    if (isPossibleToUse(contextType))
      Usage.CALLABLE
    else
      null

  private fun isPossibleToUse(contextType: ContextType): Boolean {
    val visibility = this.visibility ?: Visibility.PUBLIC
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

  override fun getIcon(flags: Int): Icon {
    specialFunction?.let { if (it == SpecialFunctionType.RECEIVE) return SolidityIcons.RECEIVE }
    val main = when (visibility) {
      Visibility.PRIVATE -> SolidityIcons.FUNCTION_PRV
      Visibility.INTERNAL -> SolidityIcons.FUNCTION_INT
      Visibility.PUBLIC -> SolidityIcons.FUNCTION_PUB
      Visibility.EXTERNAL -> SolidityIcons.FUNCTION_EXT
      null -> SolidityIcons.FUNCTION
    }
    val ext = when (mutability) {
        Mutability.PURE -> SolidityIcons.PURE
        Mutability.VIEW -> SolidityIcons.VIEW
      Mutability.PAYABLE -> SolidityIcons.PAYABLE
        null -> SolidityIcons.WRITE
      }
    return IconManager.getInstance().createRowIcon(main, ext)
  }

  companion object {
    fun parseParameters(parameters: List<SolParameterDef>): List<Pair<String?, SolType>> {
      return parameters.map { it.identifier?.text to getSolType(it.typeName) }
    }
  }
}
abstract class SolModifierDefMixin : SolStubbedNamedElementImpl<SolModifierDefStub>, SolModifierDefinition {
  override val contract: SolContractDefinition
    get() = ancestors.firstInstance()

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolModifierDefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int) = SolidityIcons.MODIFIER
}

abstract class SolStateVarDeclMixin : SolStubbedNamedElementImpl<SolStateVarDeclStub>, SolStateVariableDeclaration {
  constructor(node: ASTNode) : super(node)
  constructor(stub: SolStateVarDeclStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getIcon(flags: Int): Icon {
    return when (mutability) {
      VariableMutability.CONSTANT -> SolidityIcons.CONSTANT_VARIABLE
      VariableMutability.IMMUTABLE -> SolidityIcons.IMMUTABLE_VARIABLE
      null -> if (firstChild.text == "address payable") IconManager.getInstance().createRowIcon(SolidityIcons.STATE_VAR, SolidityIcons.PAYABLE) else SolidityIcons.STATE_VAR
    }
  }

  override fun parseParameters(): List<Pair<String?, SolType>> = emptyList()

  override fun parseType(): SolType = getSolType(typeName)

  override fun getPossibleUsage(contextType: ContextType): Usage? {
    val visibility = this.visibility ?: Visibility.INTERNAL
    return when {
        contextType == ContextType.SUPER || contextType == ContextType.BUILTIN -> Usage.VARIABLE
        contextType == ContextType.EXTERNAL && visibility == Visibility.PUBLIC -> Usage.CALLABLE
        else -> null
    }
  }

  override val callablePriority = 0

  override fun resolveElement() = this

  override val visibility: Visibility?
    get() = visibilityModifier?.text?.let { safeValueOf(it.uppercase()) }

  override val mutability: VariableMutability?
    get() = mutationModifier?.text?.let { safeValueOf(it.uppercase()) }

  override val mutationModifier: SolMutationModifier?
    get() = mutationModifierList.firstOrNull()

  override val visibilityModifier: SolVisibilityModifier?
    get() = visibilityModifierList.firstOrNull()

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
        expr.varLiteral ?: expr.stringLiteral ?: expr.numberLiteral ?: expr.hexLiteral ?: expr.booleanLiteral ?: expr.elementaryTypeName ?: expr.firstChild
      is SolMemberAccessExpression ->
        expr.identifier!!
      is SolFunctionCallExpression ->
        expr.firstChild
      is SolIndexAccessExpression ->
        expr.firstChild
      is SolNewExpression ->
        expr.typeName as PsiElement
      is SolSeqExpression ->
        expr.expressionList.firstOrNull()?.let { getReferenceNameElement(it) } ?: expr
      // unable to extract reference name element
      else -> expr
    }
  }

  override val referenceNameElement: PsiElement
    get() = getReferenceNameElement(expression)

  override val referenceName: String
    get() = referenceNameElement.text

  override fun getName(): String? = referenceName

  override fun getReference(): SolReference = SolFunctionCallReference(this as SolFunctionCallExpression)

  override val functionCallArguments: SolFunctionCallArguments
    get() = functionInvocation.functionCallArguments!!

  override fun resolveDefinitions(): List<SolCallable>? {

    return when (val it = children.firstOrNull()) {
      is SolMemberAccessExpression -> SolResolver.resolveMemberAccess(it)
      is SolNewExpression -> it.reference?.multiResolve() ?: emptyList()
      else -> SolResolver.resolveVarLiteralReference(this)
    }.filterIsInstance<SolCallable>()
  }
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

abstract class SolVariableDeclarationMixin(node: ASTNode) : SolVariableDeclaration, SolNamedElementImpl(node) {
  override fun getIcon(flags: Int): Icon = SolidityIcons.STATE_VAR
}

open class SolParameterDefMixin(node: ASTNode) : SolNamedElementImpl(node)

abstract class SolUserDefinedTypeNameImplMixin : SolStubbedElementImpl<SolTypeRefStub>, SolUserDefinedTypeName {
  constructor(node: ASTNode) : super(node)

  constructor(stub: SolTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

  override fun getReference(): SolReference = SolUserDefinedTypeNameReference(this)

  override val referenceNameElement: PsiElement
    get() = findIdentifiers().last()

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

  companion object {
    fun parseParameters(parameters: List<SolParameterDef>): List<Pair<String?, SolType>> {
      return parameters.map { it.identifier?.text to getSolType(it.typeName) }
    }
  }
}

abstract class SolMemberAccessElement(node: ASTNode) : SolNamedElementImpl(node), SolMemberAccessExpression {
  override val referenceNameElement: PsiElement
    get() = findChildByType(IDENTIFIER)!!
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolMemberAccessReference(this)

  fun collectUsingForLibraryFunctions(): List<SolFunctionDefinition> {
    val type = expression.type.takeIf { it != SolUnknown } ?: return emptyList()
    val contract = findContract()
    val superContracts = contract
      ?.collectSupers
      ?.flatMap { SolResolver.resolveTypeNameUsingImports(it) }
      ?.filterIsInstance<SolContractDefinition>()
      ?: emptyList()
    val libraries = (superContracts + contract.wrap())
            .asSequence()
            .flatMap { it.usingForDeclarationList }
      .filter {
        val usingType = it.type
        usingType == null || usingType == type
      }
      .mapNotNull { it.library }
      .distinct()
      .flatMap { it.functionDefinitionList }
            .toList()
    return libraries
  }
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
    return indexedParameterList?.indexedParamDefList
      ?.map { it.identifier?.text to getSolType(it.typeName) }
      ?: emptyList()
  }

  override fun getIcon(flags: Int): Icon? {
    return SolidityIcons.EVENT
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
    return indexedParameterList?.indexedParamDefList
      ?.map { it.identifier?.text to getSolType(it.typeName) }
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

  override fun getIcon(flags: Int): Icon = SolidityIcons.ERROR
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


abstract class SolFunctionTypeMixin : SolStubbedElementImpl<SolTypeRefStub>, SolFunctionTypeName, SolFunctionTypeElement {

  constructor(node: ASTNode) : super(node)
  constructor(stub: SolTypeRefStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)


  override val params: List<SolParameterDef>
    get() = parameterListList.first().parameterDefList
  override val returns: SolType
    get() = parameterListList.getOrNull(1).parseType()
  override val mutability: Mutability?
    get() = stateMutabilitySpecifierList.parseMutability()
  override val visibility: Visibility?
    get() = visibilitySpecifierList.parseVisibility()
}

abstract class SolYulPathElement(node: ASTNode) : SolNamedElementImpl(node), SolReferenceElement {
  override val referenceNameElement: PsiElement
    get() = firstChild
  override val referenceName: String
    get() = referenceNameElement.text

  override fun getReference() = SolYulLiteralReference(this)
}

fun List<SolVisibilitySpecifier>.parseVisibility() =
     map { it.text.uppercase() }
    .mapNotNull { safeValueOf<Visibility>(it) }
    .firstOrNull()


fun List<SolStateMutabilitySpecifier>.parseMutability() =
       map { it.text.uppercase() }
         .firstNotNullOfOrNull { safeValueOf<Mutability>(it) }

