package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.*

object SolResolver {
  fun resolveTypeNameUsingImports(element: SolReferenceElement): Set<SolNamedElement> =
    CachedValuesManager.getCachedValue(element) {
      val result = resolveContractUsingImports(element, element.containingFile, true) +
        resolveEnum(element, element.containingFile) +
        resolveStruct(element, element.containingFile)
      CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

  /**
   * @param withAliases aliases are not recursive, so count them only at the first level of recursion
   */
  private fun resolveContractUsingImports(element: SolNamedElement, file: PsiFile, withAliases: Boolean): Set<SolContractDefinition> =
    RecursionManager.doPreventingRecursion(ResolveContractKey(element.name, file), true) {
      val inFile = file.children
        .filterIsInstance<SolContractDefinition>()
        .filter { it.name == element.name }

      val resolvedViaAlias = when (withAliases) {
        true -> file.children
          .filterIsInstance<SolImportDirective>()
          .mapNotNull { directive ->
            directive.importAliasedPairList
              .firstOrNull { aliasPair -> aliasPair.importAlias?.name == element.name }
              ?.let { aliasPair ->
                directive.importPath?.reference?.resolve()?.let { resolvedFile ->
                  aliasPair.userDefinedTypeName to resolvedFile
                }
              }
          }.flatMap { (alias, resolvedFile) ->
            resolveContractUsingImports(alias, resolvedFile.containingFile, false)
          }
        else -> emptyList()
      }

      val imported = file.children
        .filterIsInstance<SolImportDirective>()
        .mapNotNull { it.importPath?.reference?.resolve() }
        .map { it.containingFile }
        .flatMap { resolveContractUsingImports(element, it, false) }

      (inFile + resolvedViaAlias + imported).toSet()
    } ?: emptySet()

  private fun resolveEnum(element: SolReferenceElement, file: PsiFile): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element, file) { it.enumDefinitionList }

  private fun resolveStruct(element: SolReferenceElement, file: PsiFile): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element, file) { it.structDefinitionList }

  private fun <T : SolNamedElement> resolveInnerType(element: SolReferenceElement, file: PsiFile, f: (SolContractDefinition) -> List<T>): Set<T> =
    RecursionManager.doPreventingRecursion(ResolveContractKey(element.name, file), true) {
      val inheritanceSpecifier = element.parentOfType<SolInheritanceSpecifier>()
      if (inheritanceSpecifier != null) {
        emptySet()
      } else {
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
      "this" -> findContract(element)
        .wrap()
      "super" -> findContract(element)
        ?.supers
        ?.flatMap { resolveTypeNameUsingImports(it) }
        ?: emptyList()
      else -> lexicalDeclarations(element)
        .filter { it.name == element.name }
        .toList()
    }
  }

  fun resolveMemberAccess(element: SolMemberAccessExpression): List<SolNamedElement> {
    val propName = element.identifier?.text
    val ref = element.expression
    return when {
      propName == null -> emptyList()
      ref is SolPrimaryExpression && ref.varLiteral?.name == "super" -> {
        val contract = findContract(ref)
        return contract?.let { resolveContractMember(it, element, true) } ?: emptyList()
      }
      else -> {
        val refType = ref.type
        when (refType) {
          is SolContract -> resolveContractMember(refType.ref, element)
          is SolStruct -> refType.ref.variableDeclarationList.filter { it.name == propName }
          is SolEnum -> refType.ref.enumValueList.filter { it.name == propName }
          else -> emptyList()
        }
      }
    }
  }

  private fun resolveContractMember(contract: SolContractDefinition, element: SolMemberAccessExpression, skipThis: Boolean = false): List<SolNamedElement> {
    val members = if (!skipThis)
      contract.stateVariableDeclarationList.filter { it.name == element.name }
    else
      emptyList()
    if (members.isNotEmpty()) {
      return members
    }
    return contract.supers
      .map { resolveTypeName(it).firstOrNull() }
      .filterIsInstance<SolContractDefinition>()
      .flatMap { resolveContractMember(it, element) }
  }

  fun resolveFunction(type: SolType, element: SolFunctionCallExpression, skipThis: Boolean = false): Collection<FunctionResolveResult> {
    if (element.argumentsNumber() == 1) {
      val contracts = resolveTypeName(element)
      if (contracts.isNotEmpty()) {
        return contracts.map { FunctionResolveResult(it) }
      }
    }
    val contract = findContract(element)
    val superContracts = contract
      ?.collectSupers
      ?.flatMap { SolResolver.resolveTypeNameUsingImports(it) }
      ?.filterIsInstance<SolContractDefinition>()
      ?: emptyList()
    val fromLibraries = (superContracts + contract.wrap())
      .flatMap { it.usingForDeclarationList }
      .filter {
        val usingType = it.type
        usingType == null || usingType == type
      }
      .map { it.library }
      .distinct()
      .flatMap { resoleFunInLibrary(element, it) }

    val fromContracts = if (type is SolContract)
      resolveFunRec(type.ref, element, skipThis)
        .map { FunctionResolveResult(it) }
    else
      emptyList()

    return fromContracts + fromLibraries
  }

  private fun resoleFunInLibrary(element: SolFunctionCallExpression, library: SolContractDefinition): Collection<FunctionResolveResult> {
    return library.functionDefinitionList
      .filter { it.name == element.referenceName &&
        it.parameterListList.firstOrNull()?.parameterDefList?.size == element.argumentsNumber() + 1}
      .map { FunctionResolveResult(it, true) }
  }

  private fun resolveFunRec(contract: SolContractDefinition, element: SolFunctionCallExpression, skipThis: Boolean = false): List<PsiElement> {
    val currentContractFunctions = if (!skipThis) contract.functionDefinitionList
      .filter {
        it.name == element.referenceName &&
          it.parameterListList.firstOrNull()?.parameterDefList?.size == element.argumentsNumber()
      }
    else
      emptyList()
    val eventDefinitions = if (!skipThis) contract.eventDefinitionList
      .filter {
        it.name == element.referenceName &&
          it.indexedParameterList?.typeNameList?.size ?: 0 == element.argumentsNumber()
      }
    else
      emptyList()
    val structDefinitions = if (!skipThis) contract.structDefinitionList
      .filter {
        it.name == element.referenceName
      }
    else
      emptyList()
    return when {
      currentContractFunctions.isNotEmpty() -> currentContractFunctions
      eventDefinitions.isNotEmpty() -> eventDefinitions
      structDefinitions.isNotEmpty() -> structDefinitions
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
    return lexicalDeclarations(globalType.ref, place) + lexicalDeclRec(place, stop).distinct()
  }

  private fun lexicalDeclRec(place: PsiElement, stop: (PsiElement) -> Boolean): Sequence<SolNamedElement> {
    return place.ancestors
      .drop(1) // current element might not be a SolElement
      .takeWhileInclusive { it is SolElement && !stop(it) }
      .flatMap { lexicalDeclarations(it, place) }
  }

  private fun lexicalDeclarations(scope: PsiElement, place: PsiElement): Sequence<SolNamedElement> {
    return when (scope) {
      is SolVariableDeclaration -> {
        scope.declarationList?.declarationItemList?.filterIsInstance<SolNamedElement>()?.asSequence()
          ?: scope.typedDeclarationList?.typedDeclarationItemList?.filterIsInstance<SolNamedElement>()?.asSequence()
          ?: sequenceOf(scope)
      }
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
        scope.parameters.asSequence() +
          (scope.returns?.parameterDefList?.asSequence() ?: emptySequence())
      }
      is SolConstructorDefinition -> {
        scope.parameterList?.parameterDefList?.asSequence() ?: emptySequence()
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

      is SolTupleStatement -> {
        scope.variableDeclaration?.let {
          val declarationList = it.declarationList
          val typedDeclarationList = it.typedDeclarationList
          when {
            declarationList != null -> declarationList.declarationItemList.asSequence()
            typedDeclarationList != null -> typedDeclarationList.typedDeclarationItemList.asSequence()
            else -> emptySequence()
          }
        } ?: emptySequence()
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

data class ResolveContractKey(val name: String?, val file: PsiFile)

data class FunctionResolveResult(val psiElement: PsiElement, val usingLibrary: Boolean = false)

private fun <T> Sequence<T>.takeWhileInclusive(pred: (T) -> Boolean): Sequence<T> {
  var shouldContinue = true
  return takeWhile {
    val result = shouldContinue
    shouldContinue = pred(it)
    result
  }
}
