package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.*

object SolResolver {
  fun resolveTypeNameUsingImports(element: SolReferenceElement): Set<SolNamedElement> =
    resolveContractUsingImports(element, element.containingFile) +
      resolveEnum(element, element.containingFile) +
      resolveStruct(element, element.containingFile)

  private fun resolveContractUsingImports(element: SolReferenceElement, file: PsiFile): Set<SolContractDefinition> =
    RecursionManager.doPreventingRecursion(file, true) {
      val inFile = file.children
        .filterIsInstance<SolContractDefinition>()
        .filter { it.name == element.name }

      val imported = file.children
        .filterIsInstance<SolImportDirective>()
        .mapNotNull { it.importPath?.reference?.resolve() }
        .map { it.containingFile }
        .flatMap { resolveContractUsingImports(element, it) }

      (inFile + imported).toSet()
    } ?: emptySet()

  private fun resolveEnum(element: SolReferenceElement, file: PsiFile): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element, file, { it.enumDefinitionList } )

  private fun resolveStruct(element: SolReferenceElement, file: PsiFile): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element, file, { it.structDefinitionList } )

  private fun <T : SolNamedElement> resolveInnerType(element: SolReferenceElement, file: PsiFile, f: (SolContractDefinition) -> List<T>): Set<T> =
    RecursionManager.doPreventingRecursion(file, true) {
      val contract = element.parentOfType<SolContractDefinition>()
      if (contract == null) {
        emptySet()
      } else {
        val supers = contract.collectSupers
          .mapNotNull { it.reference?.resolve() }.filterIsInstance<SolContractDefinition>() + contract
        supers.flatMap(f)
          .filter { it.name == element.name }
          .toSet()
      }
    } ?: emptySet()

  fun resolveTypeName(element: SolReferenceElement): Collection<SolNamedElement> = StubIndex.getElements(
    SolGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolNamedElement::class.java
  )

  fun resolveModifier(modifier: PsiElement): List<SolModifierDefinition> = StubIndex.getElements(
    SolModifierIndex.KEY,
    modifier.text,
    modifier.project,
    null,
    SolNamedElement::class.java
  ).filterIsInstance<SolModifierDefinition>()
    .toList()

  fun resolveVarLiteral(element: SolNamedElement): List<SolNamedElement> {
    return when (element.name) {
      "this" -> {
        element.ancestors
          .filterIsInstance<SolContractDefinition>()
          .firstOrNull()
          .wrap()
      }
      "super" -> {
        element.ancestors
          .filterIsInstance<SolContractDefinition>()
          .map { it.supers.firstOrNull() }
          .filterNotNull()
          .flatMap { resolveTypeName(it).asSequence() }
          .firstOrNull().wrap()
      }
      else -> lexicalDeclarations(element)
        .filter { it.name == element.name }
        .toList()
    }
  }

  fun resolveMemberAccess(element: SolMemberAccessExpression): List<SolNamedElement> {
    val propName = element.identifier?.text
    val refType = element.expression.type
    return when {
      propName == null -> emptyList()
      refType is SolContract -> resolveContractMember(refType.ref, element)
      refType is SolStruct -> refType.ref.variableDeclarationList.filter { it.name == propName }
      refType is SolEnum -> refType.ref.enumValueList.filter { it.name == propName }
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

  fun resolveFunction(contract: SolContractDefinition, element: SolFunctionCallExpression): Collection<PsiElement> {
    if (element.argumentsNumber() == 1) {
      val contracts = resolveTypeName(element)
      if (contracts.isNotEmpty()) {
        return contracts
      }
    }
    return resolveFunRec(contract, element)
  }

  private fun resolveFunRec(contract: SolContractDefinition, element: SolFunctionCallExpression): List<PsiElement> {
    val currentContractFunctions = contract.functionDefinitionList
      .filter {
        it.name == element.referenceName &&
          it.parameterListList[0].parameterDefList.size == element.argumentsNumber()
      }
    val eventDefinitions = contract.eventDefinitionList
      .filter {
        it.name == element.referenceName &&
          it.indexedParameterList?.typeNameList?.size ?: 0 == element.argumentsNumber()
      }
    return when {
      currentContractFunctions.isNotEmpty() -> currentContractFunctions
      eventDefinitions.isNotEmpty() -> eventDefinitions
      else -> contract.supers.asSequence()
        .flatMap { resolveTypeName(it).asSequence() }
        .filterIsInstance<SolContractDefinition>()
        .map { resolveFunRec(it, element) }
        .filter { it.isNotEmpty() }
        .firstOrElse(emptyList())
      }
  }

  fun lexicalDeclarations(place: PsiElement, stop: (PsiElement) -> Boolean = { false }): Sequence<SolNamedElement> {
    val globalType = SolInternalTypeFactory.of(place.project).globalType
    return lexicalDeclarations(globalType.ref, place) + lexicalDeclRec(place, stop)
  }

  private fun lexicalDeclRec(place: PsiElement, stop: (PsiElement) -> Boolean): Sequence<SolNamedElement> {
    return place.ancestors
      .drop(1) // current element might not be a SolElement
      .takeWhileInclusive { it is SolElement && !stop(it) }
      .flatMap { lexicalDeclarations(it, place) }
  }

  private fun lexicalDeclarations(scope: PsiElement, place: PsiElement): Sequence<SolNamedElement> {
    return when (scope) {
      is SolVariableDeclaration -> sequenceOf(scope)
      is SolVariableDefinition -> lexicalDeclarations(scope.firstChild, place)

      is SolStateVariableDeclaration -> sequenceOf(scope)
      is SolContractDefinition -> {
        val childrenScope = sequenceOf(
          scope.stateVariableDeclarationList,
          scope.enumDefinitionList,
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
      is SolEnumDefinition -> sequenceOf(scope)

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

  private fun SolFunctionCallExpression.argumentsNumber() = functionCallArguments?.expressionList?.size ?: 0

  private fun <T> T?.wrap(): List<T> {
    return when (this) {
      null -> listOf()
      else -> listOf(this)
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
