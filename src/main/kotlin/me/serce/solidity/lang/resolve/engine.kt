package me.serce.solidity.lang.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.SolStruct
import me.serce.solidity.lang.types.type

object SolResolver {
  fun resolveTypeName(element: SolUserDefinedTypeName): List<SolNamedElement> = StubIndex.getElements(
    SolGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolNamedElement::class.java
  ).toList()

  fun resolveModifier(modifier: PsiElement): List<SolNamedElement> = StubIndex.getElements(
    SolModifierIndex.KEY,
    modifier.text,
    modifier.project,
    null,
    SolNamedElement::class.java
  ).toList()

  fun resolveVarLiteral(element: SolVarLiteral): List<SolNamedElement> {
    if (element.name == "this") {
      val firstContact = element.ancestors
        .asSequence()
        .filterIsInstance<SolContractDefinition>()
        .firstOrNull()
      return when (firstContact) {
        null -> listOf()
        else -> listOf(firstContact)
      }
    }
    return lexicalDeclarations(element)
      .filter { it.name == element.name }
      .toList()
  }

  fun resolveMemberAccess(element: SolMemberAccessExpression): List<SolNamedElement> {
    val propName = element.identifier?.text
    val refType = element.expression.type
    return when {
      propName == null -> emptyList()
      refType is SolContract -> resolveContractMember(refType.ref, element)
      refType is SolStruct -> refType.ref.variableDeclarationList.filter { it.name == propName }
      else -> emptyList()
    }
  }

  private fun resolveContractMember(ref: SolContractDefinition, element: SolMemberAccessExpression): List<SolNamedElement> {
    val members = ref.stateVariableDeclarationList.filter { it.name == element.name }
    if (members.isNotEmpty()) {
      return members
    }
    return ref.supers
      .map { resolveTypeName(it).firstOrNull() }
      .filterIsInstance<SolContractDefinition>()
      .flatMap { resolveContractMember(it, element) }
  }

  fun resolveFunction(contract: SolContractDefinition, element: SolFunctionCallExpression): List<PsiElement> {
    val currentContractFunctions = contract.functionDefinitionList
      .filter {
        it.name == element.referenceName &&
        it.parameterListList[0].parameterDefList.size == element.functionCallArguments?.expressionList?.size ?: 0
      }
    return when {
      currentContractFunctions.isNotEmpty() -> currentContractFunctions
      else -> contract.supers.asSequence()
        .flatMap { resolveTypeName(it).asSequence() }
        .filterIsInstance<SolContractDefinition>()
        .map { resolveFunction(it, element) }
        .filter { it.isNotEmpty() }
        .firstOrElse(emptyList())
    }
  }

  fun lexicalDeclarations(place: SolElement, stop: (PsiElement) -> Boolean = { false }): Sequence<SolNamedElement> =
    place.ancestors
      .takeWhileInclusive { it is SolElement && !stop(it) }
      .flatMap { lexicalDeclarations(it, place) }

  private fun lexicalDeclarations(scope: PsiElement, place: SolElement): Sequence<SolNamedElement> {
    return when (scope) {
      is SolVariableDeclaration -> sequenceOf(scope)
      is SolVariableDefinition -> lexicalDeclarations(scope.firstChild, place)

      is SolStateVariableDeclaration -> sequenceOf(scope)
      is SolContractDefinition -> {
        val childrenScope = sequenceOf(
          scope.stateVariableDeclarationList,
          scope.structDefinitionList).flatten()
          .map { lexicalDeclarations(it, place) }
          .flatten()
        val extendsScope = scope.supers.asSequence()
          .map { resolveTypeName(it).firstOrNull() }
          .filterNotNull()
          .map { lexicalDeclarations(it, place) }
          .flatten()
        childrenScope + extendsScope
      }
      is SolFunctionDefinition -> {
        scope.parameters.asSequence()
      }

      is SolStatement -> {
        scope.children.asSequence()
          .map { lexicalDeclarations(it, place) }
          .flatten()
      }

      is SolBlock -> {
        scope.statementList.asSequence()
          .map { lexicalDeclarations(it, place) }
          .flatten()
      }

      else -> emptySequence()
    }
  }

}

private fun <T> Sequence<T>.takeWhileInclusive(pred: (T) -> Boolean): Sequence<T> {
  var shouldContinue = true
  return takeWhile {
    val result = shouldContinue
    shouldContinue = pred(it)
    result
  }
}
