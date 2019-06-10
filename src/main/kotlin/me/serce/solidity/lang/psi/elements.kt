package me.serce.solidity.lang.psi

import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import me.serce.solidity.lang.resolve.ref.SolReference
import me.serce.solidity.lang.types.SolType

interface SolElement : PsiElement {
  override fun getReference(): PsiReference?
}

interface SolNamedElement : SolElement, PsiNamedElement, NavigatablePsiElement

interface SolFunctionDefElement : SolReferenceElement {
  val contract: SolContractDefinition
  val modifiers: List<SolModifierInvocation>
  val parameters: List<SolParameterDef>
  val parameterTypes: List<SolType>
  val returns: SolParameterList?
  val isConstructor: Boolean
}

interface SolFunctionCallElement : SolReferenceElement {
  val expression: SolExpression
  val functionCallArguments: SolFunctionCallArguments

  fun getBaseAndReferenceNameElement(): Pair<SolExpression?, PsiElement>
}

interface SolModifierInvocationElement : SolReferenceElement {
  val varLiteral: SolVarLiteral
  val functionCallArguments: SolFunctionCallArguments
}

interface SolEnumDefElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolModifierElement : SolNamedElement {
  val contract: SolContractDefinition
}

interface SolContractOrLibElement : SolNamedElement {
  val supers: List<SolUserDefinedTypeName>
  val collectSupers: Collection<SolUserDefinedTypeName>
}

interface SolReferenceElement : SolNamedElement {
  val referenceNameElement: PsiElement
  val referenceName: String

  override fun getReference(): SolReference?
}

interface SolUsingForElement : PsiElement {
  val type: SolType?
  val library: SolContractDefinition
  fun getTypeNameList(): List<SolTypeName>
}
