package me.serce.solidity.lang.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import me.serce.solidity.lang.resolve.ref.SolReference
import me.serce.solidity.lang.types.SolMember
import me.serce.solidity.lang.types.SolType

interface SolElement : PsiElement {
  override fun getReference(): PsiReference?
}

interface SolNamedElement : SolElement, PsiNamedElement, NavigatablePsiElement

enum class Visibility {
  PRIVATE,
  INTERNAL,
  PUBLIC,
  EXTERNAL
}

enum class Mutability {
  PURE, CONSTANT, VIEW, PAYABLE;
}

enum class ContractType(val docName: String) {
  COMMON("contract"), LIBRARY("library"), INTERFACE("interface")
}

interface SolCallable {
  val callablePriority: Int
  fun getName(): String?
  fun parseType(): SolType
  fun parseParameters(): List<Pair<String?, SolType>>
  fun resolveElement(): SolNamedElement?
}

interface SolCallableElement : SolCallable, SolNamedElement

interface SolStateVarElement : SolMember, SolCallableElement {
  val visibilityModifier: SolVisibilityModifier?
  val visibility: Visibility?

  val mutationModifier: SolMutationModifier?
  val mutability: Mutability?
}

interface SolConstantVariable : SolNamedElement {}


enum class SpecialFunctionType {
  RECEIVE, FALLBACK
}

interface SolFunctionDefElement : SolHasModifiersElement, SolMember, SolCallableElement {
  /** The contract can be null in the case of free functions. */
  val contract: SolContractDefinition?
  val modifiers: List<SolModifierInvocation>
  val parameters: List<SolParameterDef>
  val returns: SolParameterList?
  val isConstructor: Boolean
  val visibility: Visibility?
  val mutability: Mutability?
  val specialFunction: SpecialFunctionType?
}

inline fun <reified T : Enum<*>> safeValueOf(name: String): T? =
  T::class.java.enumConstants.firstOrNull { it.name == name }

interface SolFunctionCallElement : SolReferenceElement {
  val expression: SolExpression?
  val functionCallArguments: SolFunctionCallArguments
}

interface SolModifierInvocationElement : SolReferenceElement {
  val varLiteral: SolVarLiteral
  val functionCallArguments: SolFunctionCallArguments
}

interface SolEnumDefElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolUserDefinedValueTypeDefElement : SolNamedElement

interface SolEnumItemElement : SolEnumDefElement, SolMember

interface SolModifierElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolContractOrLibElement : SolCallableElement {
  val supers: List<SolUserDefinedTypeName>
  val collectSupers: Collection<SolUserDefinedTypeName>
  val isAbstract: Boolean
  val contractType: ContractType
  val isPayable: Boolean
}

interface SolReferenceElement : SolNamedElement {
  val referenceNameElement: PsiElement
  val referenceName: String

  override fun getReference(): SolReference?
}

interface SolUserDefinedTypeNameElement : SolReferenceElement {
  fun findIdentifiers(): List<PsiElement>
}

interface SolHasModifiersElement : SolReferenceElement

interface SolUsingForElement : PsiElement {
  val type: SolType?
  val library: SolContractDefinition?
  fun getTypeNameList(): List<SolTypeName>
}
