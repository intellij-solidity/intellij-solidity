package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.*
import me.serce.solidity.nullIfError
import me.serce.solidity.wrap

object SolResolver {
  fun resolveTypeNameUsingImports(element: SolReferenceElement): Set<SolNamedElement> = CachedValuesManager.getCachedValue(element) {
    val result = resolveContractUsingImports(element, element.containingFile, true) +
      resolveEnum(element) +
      resolveStruct(element)
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
        .mapNotNull { nullIfError { it.importPath?.reference?.resolve()?.containingFile } }
        .flatMap { resolveContractUsingImports(element, it, false) }

      (inFile + resolvedViaAlias + imported).toSet()
    } ?: emptySet()

  private fun resolveEnum(element: SolReferenceElement): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element) { it.enumDefinitionList }

  private fun resolveStruct(element: SolReferenceElement): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element) { it.structDefinitionList }

  private fun <T : SolNamedElement> resolveInnerType(element: SolReferenceElement, f: (SolContractDefinition) -> List<T>): Set<T> {
    val inheritanceSpecifier = element.parentOfType<SolInheritanceSpecifier>()
    return if (inheritanceSpecifier != null) {
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
  }

  fun resolveTypeName(element: SolReferenceElement): Collection<SolNamedElement> = StubIndex.getElements(
    SolGotoClassIndex.KEY,
    element.referenceName,
    element.project,
    null,
    SolNamedElement::class.java
  )

  fun resolveModifier(modifier: SolModifierInvocationElement): List<SolModifierDefinition> = StubIndex.getElements(
    SolModifierIndex.KEY,
    modifier.text,
    modifier.project,
    null,
    SolNamedElement::class.java
  ).filterIsInstance<SolModifierDefinition>()
    .toList()

  fun resolveVarLiteral(element: SolNamedElement): List<SolNamedElement> {
    return when (element.name) {
      "this" -> element.findContract()
        .wrap()
      "super" -> element.findContract()
        ?.supers
        ?.flatMap { resolveTypeNameUsingImports(it) }
        ?: emptyList()
      else -> lexicalDeclarations(element)
        .filter { it.name == element.name }
        .toList()
    }
  }

  fun resolveMemberAccess(element: SolMemberAccessExpression): List<SolNamedElement> {
    val functionCall = element.findParentOrNull<SolFunctionCallElement>()
    return if (functionCall != null) {
      val resolved = functionCall.reference?.multiResolve() ?: emptyList()
      if (resolved.isNotEmpty()) {
        resolved.filterIsInstance<SolNamedElement>()
      } else {
        resolveMembers(element)
      }
    } else {
      resolveMembers(element)
    }
  }

  fun resolveMembers(element: SolMemberAccessExpression): List<SolNamedElement> {
    val memberName = element.identifier?.text
    val ref = element.expression
    return when {
      memberName == null -> emptyList()
      ref is SolPrimaryExpression && ref.varLiteral?.name == "super" -> {
        val contract = ref.findContract()
        return contract?.let { resolveContractMember(it, element, true) } ?: emptyList()
      }
      else -> {
        when (val refType = ref.type) {
          is SolContract -> resolveContractMember(refType.ref, element)
          is SolStruct -> refType.ref.variableDeclarationList.filter { it.name == memberName }
          is SolEnum -> refType.ref.enumValueList.filter { it.name == memberName }
          else -> emptyList()
        }
      }
    }
  }

  private fun resolveContractMember(
    contract: SolContractDefinition,
    element: SolMemberAccessExpression,
    skipThis: Boolean = false): List<SolNamedElement> {

    val members = if (!skipThis)
      (contract.stateVariableDeclarationList as List<SolNamedElement> + contract.functionDefinitionList)
        .filter { it.name == element.name }
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
          .flatten() + scope.structDefinitionList + scope.eventDefinitionList
        val extendsScope = scope.supers.asSequence()
          .map { resolveTypeName(it).firstOrNull() }
          .filterNotNull()
          .map { lexicalDeclarations(it, place) }
          .flatten()
        childrenScope + extendsScope + scope.functionDefinitionList
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

      is SolidityFile -> {
        RecursionManager.doPreventingRecursion(scope.name, true) {
          val contracts = scope.children.asSequence()
            .filterIsInstance<SolContractDefinition>()
          val imports = scope.children.asSequence().filterIsInstance<SolImportDirective>()
            .mapNotNull { nullIfError { it.importPath?.reference?.resolve()?.containingFile } }
            .mapNotNull { lexicalDeclarations(it, place) }
            .flatten()
          imports + contracts
        } ?: emptySequence()
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
}

data class ResolveContractKey(val name: String?, val file: PsiFile)

private fun <T> Sequence<T>.takeWhileInclusive(pred: (T) -> Boolean): Sequence<T> {
  var shouldContinue = true
  return takeWhile {
    val result = shouldContinue
    shouldContinue = pred(it)
    result
  }
}

fun ResolvedCallable.canBeApplied(arguments: SolFunctionCallArguments) : Boolean {
  val callArgumentTypes = arguments.expressionList.map { it.type }
  val parameters = parseParameters()
    .map { it.second }
  if (parameters.size != callArgumentTypes.size)
    return false
  return !parameters.zip(callArgumentTypes)
    .any { (paramType, argumentType) ->
      paramType != SolUnknown && !paramType.isAssignableFrom(argumentType)
    }
}
