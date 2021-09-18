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
  val visibility: Visibility
}

interface SolConstantVariable : SolNamedElement {}

interface SolFunctionDefElement : SolHasModifiersElement, SolMember, SolCallableElement {
  /** The contract can be null in the case of free functions. */
  val contract: SolContractDefinition?
  val modifiers: List<SolModifierInvocation>
  val parameters: List<SolParameterDef>
  val returns: SolParameterList?
  val isConstructor: Boolean
  val visibility: Visibility
}

inline fun <reified T : Enum<*>> safeValueOf(name: String): T? =
  T::class.java.enumConstants.firstOrNull { it.name == name }

interface SolFunctionCallElement : SolReferenceElement {
  val expression: SolExpression?
  val functionCallArguments: SolFunctionCallArguments

  fun getBaseAndReferenceNameElement(): Pair<SolExpression?, PsiElement>?
}

interface SolModifierInvocationElement : SolReferenceElement {
  val varLiteral: SolVarLiteral
  val functionCallArguments: SolFunctionCallArguments
}

interface SolEnumDefElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolEnumItemElement : SolEnumDefElement, SolMember

interface SolModifierElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolContractOrLibElement : SolCallableElement {
  val supers: List<SolUserDefinedTypeName>
  val collectSupers: Collection<SolUserDefinedTypeName>
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
  val library: SolContractDefinition
  fun getTypeNameList(): List<SolTypeName>
}
