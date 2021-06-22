package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.types.*
import me.serce.solidity.nullIfError
import me.serce.solidity.wrap

object SolResolver {
  fun resolveTypeNameUsingImports(element: PsiElement): Set<SolNamedElement> = CachedValuesManager.getCachedValue(element) {
    val result = resolveContractUsingImports(element, element.containingFile, true) +
      resolveEnum(element) +
      resolveStruct(element)
    CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
  }

  /**
   * @param withAliases aliases are not recursive, so count them only at the first level of recursion
   */
  private fun resolveContractUsingImports(element: PsiElement, file: PsiFile, withAliases: Boolean): Set<SolContractDefinition> =
    RecursionManager.doPreventingRecursion(ResolveContractKey(element.nameOrText, file), true) {
      if (element is SolUserDefinedTypeName && element.findIdentifiers().size > 1) {
        emptySet()
      } else {
        val inFile = file.children
          .filterIsInstance<SolContractDefinition>()
          .filter { it.name == element.nameOrText }

        val resolvedViaAlias = when (withAliases) {
          true -> file.children
            .filterIsInstance<SolImportDirective>()
            .mapNotNull { directive ->
              directive.importAliasedPairList
                .firstOrNull { aliasPair -> aliasPair.importAlias?.name == element.nameOrText }
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
      }
    } ?: emptySet()

  private fun resolveEnum(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element) { it.enumDefinitionList }

  private fun resolveStruct(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element) { it.structDefinitionList }

  private fun <T : SolNamedElement> resolveInnerType(element: PsiElement, f: (SolContractDefinition) -> List<T>): Set<T> {
    val inheritanceSpecifier = element.parentOfType<SolInheritanceSpecifier>()
    return if (inheritanceSpecifier != null) {
      emptySet()
    } else {
      val names = if (element is SolUserDefinedTypeNameElement) {
        element.findIdentifiers()
      } else {
        element.wrap()
      }
      when {
        names.size > 2 -> emptySet()
        names.size > 1 -> resolveTypeNameUsingImports(names[0])
          .filterIsInstance<SolContractDefinition>()
          .firstOrNull()
          ?.let { resolveInnerType(it, names[1].nameOrText!!, f) }
          ?: emptySet()

        else -> element.parentOfType<SolContractDefinition>()
          ?.let {
            names[0].nameOrText?.let { nameOrText ->
              resolveInnerType(it, nameOrText, f)
            }
          }
          ?: emptySet()
      }
    }
  }

  private val PsiElement.nameOrText
    get() = if (this is PsiNamedElement) {
      this.name
    } else {
      this.text
    }

  private fun <T : SolNamedElement> resolveInnerType(contract: SolContractDefinition, name: String, f: (SolContractDefinition) -> List<T>): Set<T> {
    val supers = contract.collectSupers
      .mapNotNull { it.reference?.resolve() }.filterIsInstance<SolContractDefinition>() + contract
    return supers.flatMap(f)
      .filter { it.name == name }
      .toSet()
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

  fun resolveVarLiteralReference(element: SolNamedElement): List<SolNamedElement> {
    return if (element.parent?.parent is SolFunctionCallExpression) {
      val functionCall = element.findParentOrNull<SolFunctionCallElement>()!!
      val resolved = functionCall.reference?.multiResolve() ?: emptyList()
      if (resolved.isNotEmpty()) {
        resolved.filterIsInstance<SolNamedElement>()
      } else {
        resolveVarLiteral(element)
      }
    } else {
      resolveVarLiteral(element)
        .findBest {
          when (it) {
            is SolStateVariableDeclaration -> 0
            else -> Int.MAX_VALUE
          }
        }
    }
  }

  private fun <T : Any> List<T>.findBest(priorities: (T) -> Int): List<T> {
    return this
      .groupBy { priorities(it) }
      .minBy { it.key }
      ?.value
      ?: emptyList()
  }

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

  fun resolveMemberAccess(element: SolMemberAccessExpression): List<SolMember> {
    if (element.parent is SolFunctionCallExpression) {
      val functionCall = element.findParentOrNull<SolFunctionCallElement>()!!
      val resolved = (functionCall.reference as SolFunctionCallReference)
        .resolveFunctionCallAndFilter()
        .filterIsInstance<SolMember>()
      if (resolved.isNotEmpty()) {
        return resolved
      }
    }
    return when (val memberName = element.identifier?.text) {
      null -> emptyList()
      else -> element.expression.getMembers()
        .filter { it.getName() == memberName }
    }
  }

  fun resolveContractMembers(contract: SolContractDefinition, skipThis: Boolean = false): List<SolMember> {
    val members = if (!skipThis)
      contract.stateVariableDeclarationList as List<SolMember> + contract.functionDefinitionList
    else
      emptyList()
    return members + contract.supers
      .map { resolveTypeName(it).firstOrNull() }
      .filterIsInstance<SolContractDefinition>()
      .flatMap { resolveContractMembers(it) }
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
          scope.stateVariableDeclarationList as List<PsiElement>,
          scope.enumDefinitionList,
          scope.structDefinitionList).flatten()
          .map { lexicalDeclarations(it, place) }
          .flatten() + scope.structDefinitionList + scope.eventDefinitionList + scope.errorDefinitionList
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

          // NOTE: Imports are intentionally resolved eagerly rather than lazily to ensure that
          // cyclic imports don't cause infinite recursion.
          val imports = scope.children.asSequence().filterIsInstance<SolImportDirective>()
            .mapNotNull { nullIfError { it.importPath?.reference?.resolve()?.containingFile } }
            .mapNotNull { lexicalDeclarations(it, place) }
            .flatten()
            .toList()
            .asSequence()
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

fun SolCallable.canBeApplied(arguments: SolFunctionCallArguments): Boolean {
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
