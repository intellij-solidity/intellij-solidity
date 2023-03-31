package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.*
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolNewExpressionElement
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.stubs.SolNamedElementIndex
import me.serce.solidity.lang.types.*
import me.serce.solidity.wrap

object SolResolver {
  fun resolveTypeNameUsingImports(element: PsiElement): Set<SolNamedElement> =
    CachedValuesManager.getCachedValue(element) {
      val result = if (element is SolFunctionCallElement) {
        resolveError(element) +
          resolveEvent(element) +
          resolveContract(element) +
          resolveEnum(element) +
          resolveUserDefinedValueType(element)
      } else {
        resolveContract(element) +
          resolveEnum(element) +
          resolveStruct(element) +
          resolveUserDefinedValueType(element)
      }
      CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

  /**
   * @param withAliases aliases are not recursive, so count them only at the first level of recursion
   */
  private fun <T : SolNamedElement> resolveUsingImports(
    target: Class<T>,
    element: PsiElement,
    file: PsiFile,
    withAliases: Boolean,
  ): Set<T> {
    // If the elements has no name or text, we can't resolve it.
    val elementName = element.nameOrText
    if (elementName == null) {
      return emptySet()
    }

    // Resolve aliases of the following form:
    // import {Wallet as ExternalWallet} from "./wallet.sol";
    val resolvedViaAlias = when {
      withAliases -> {
        val imports = file.childrenOfType<SolImportDirective>()
        imports.mapNotNull { directive ->
          directive.importAliasedPairList //
            .firstOrNull { aliasPair -> aliasPair.importAlias?.name == element.nameOrText } //
            ?.let { aliasPair ->
              directive.importPath?.reference?.resolve()?.let { resolvedFile ->
                aliasPair.userDefinedTypeName to resolvedFile
              }
            }
        }.flatMap { (alias, resolvedFile) ->
          resolveUsingImports(target, alias, resolvedFile.containingFile, false)
        }
      }

      else -> emptyList()
    }

    // Retrieve all PSI elements with the name we're trying to lookup.
    val elements: Collection<SolNamedElement> = StubIndex.getElements( //
      SolNamedElementIndex.KEY, //
      elementName, //
      element.project, //
      null, //
      SolNamedElement::class.java //
    )

    val resolvedImportedFiles = collectImports(file)
    val sameNameReferences = elements.filterIsInstance(target).filter {
      val containingFile = it.containingFile
      // During completion, IntelliJ copies PSI files, and therefore we need to ensure that we compare
      // files against its original file.
      val originalFile = file.originalFile
      // Below, either include
      containingFile == originalFile || containingFile in resolvedImportedFiles
    }
    return (sameNameReferences + resolvedViaAlias).toSet()
  }

  /**
   * Collects imports of all declarations for a given file recursively.
   */
  private fun collectImports(file: PsiFile, visited: MutableSet<PsiFile> = hashSetOf()): Collection<PsiFile> {
    if (!visited.add(file)) {
      return emptySet()
    }
    // TODO: the below code includes all declarations and ignores named imports, e.g. like the one below
    //   import {a as A} from "./a.sol";
    //
    val imports = file.childrenOfType<SolImportDirective>()
    val resolvedImportedFiles = imports.mapNotNull {
      it.importPath?.reference?.resolve()?.containingFile
    }
    return resolvedImportedFiles + resolvedImportedFiles.map { collectImports(it, visited) }.flatten()
  }

  private fun resolveContract(element: PsiElement): Set<SolContractDefinition> =
    resolveUsingImports(SolContractDefinition::class.java, element, element.containingFile, true)
  private fun resolveEnum(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element) { it.enumDefinitionList } + resolveUsingImports(SolEnumDefinition::class.java, element, element.containingFile, true)

  private fun resolveStruct(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element) { it.structDefinitionList } + resolveUsingImports(SolStructDefinition::class.java, element, element.containingFile, true)

  private fun resolveUserDefinedValueType(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolUserDefinedValueTypeDefinition>(
      element,
      { it.userDefinedValueTypeDefinitionList }) + resolveUsingImports(SolUserDefinedValueTypeDefinition::class.java, element, element.containingFile, true)

  private fun resolveEvent(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolEventDefinition>(element) { it.eventDefinitionList }

  private fun resolveError(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolErrorDefinition>(element) { it.errorDefinitionList } + resolveUsingImports(SolErrorDefinition::class.java, element, element.containingFile, true)

  private inline fun <reified T : SolNamedElement> resolveInFile(element: PsiElement) : Set<T> {
    return element.parentOfType<SolidityFile>()
      ?.children
      ?.filterIsInstance<T>()
      ?.filter { it.name == element.text }
      ?.toSet() ?: emptySet()
  }

  private fun <T : SolNamedElement> resolveInnerType(
    element: PsiElement,
    f: (SolContractDefinition) -> List<T>
  ): Set<T> {
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

  private fun <T : SolNamedElement> resolveInnerType(
    contract: SolContractDefinition,
    name: String,
    f: (SolContractDefinition) -> List<T>
  ): Set<T> {
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
    modifier.firstChild.text,
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
      .minByOrNull { it.key }
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
    val visitedScopes = hashSetOf<Pair<PsiElement, PsiElement>>()
    return lexicalDeclarations0(visitedScopes, place, stop)
  }

  private fun lexicalDeclarations0(
    visitedScopes: HashSet<Pair<PsiElement, PsiElement>>,
    place: PsiElement,
    stop: (PsiElement) -> Boolean = { false }
  ): Sequence<SolNamedElement> {
    val globalType = SolInternalTypeFactory.of(place.project).globalType
    return lexicalDeclarations(visitedScopes, globalType.ref, place) + lexicalDeclRec(visitedScopes, place, stop).distinct()
  }

  private fun lexicalDeclRec(
    visitedScopes: HashSet<Pair<PsiElement, PsiElement>>,
    place: PsiElement,
    stop: (PsiElement) -> Boolean
  ): Sequence<SolNamedElement> {
    return place.ancestors
      .drop(1) // current element might not be a SolElement
      .takeWhileInclusive { it is SolElement && !stop(it) }
      .flatMap { lexicalDeclarations(visitedScopes, it, place) }
  }

  private fun lexicalDeclarations(
    visitedScopes: HashSet<Pair<PsiElement, PsiElement>>,
    scope: PsiElement,
    place: PsiElement
  ): Sequence<SolNamedElement> {
    // Note that in some cases, loops are possible to encounter when searching for definitions.
    // To avoid the issue, ensure that we only visit place that haven't been visited before.
    if (!visitedScopes.add(scope to place)) {
      return emptySequence()
    }
    return when (scope) {
      is SolVariableDeclaration -> {
        scope.declarationList?.declarationItemList?.filterIsInstance<SolNamedElement>()?.asSequence()
          ?: scope.typedDeclarationList?.typedDeclarationItemList?.filterIsInstance<SolNamedElement>()?.asSequence()
          ?: sequenceOf(scope)
      }

      is SolVariableDefinition -> lexicalDeclarations(visitedScopes, scope.firstChild, place)

      is SolStateVariableDeclaration -> sequenceOf(scope)
      is SolContractDefinition -> {
        val childrenScope = sequenceOf(
          scope.stateVariableDeclarationList as List<PsiElement>,
          scope.enumDefinitionList,
          scope.structDefinitionList
        ).flatten()
          .map { lexicalDeclarations(visitedScopes, it, place) }
          .flatten() + scope.structDefinitionList + scope.eventDefinitionList + scope.errorDefinitionList
        val extendsScope = scope.supers.asSequence()
          .map { resolveTypeName(it).firstOrNull() }
          .filterNotNull()
          .map { lexicalDeclarations(visitedScopes, it, place) }
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
      is SolForStatement -> when {
          PsiTreeUtil.isAncestor(scope, place, false) -> {
            scope.children.firstOrNull()
              ?.let { lexicalDeclarations(visitedScopes, it, place) } ?: emptySequence()
          }
          else -> emptySequence()
      }

      is SolStatement -> {
        scope.children.asSequence()
          .map { lexicalDeclarations(visitedScopes, it, place) }
          .flatten()
      }

      is SolBlock -> {
        scope.statementList.asSequence()
          .map { lexicalDeclarations(visitedScopes, it, place) }
          .flatten()
      }

      is SolidityFile -> {
        RecursionManager.doPreventingRecursion(scope.name, true) {
          val contracts = scope.children.asSequence()
            .filterIsInstance<SolContractDefinition>()

          val constantVariables = scope.children.asSequence()
            .filterIsInstance<SolConstantVariable>()

          val freeFunctions = scope.children.asSequence()
            .filterIsInstance<SolFunctionDefinition>()

          val imports = scope.children.asSequence().filterIsInstance<SolImportDirective>()
            .mapNotNull {  it.importPath?.reference?.resolve()?.containingFile }
            .map { lexicalDeclarations(visitedScopes, it, place) }
            .flatten()
          imports + contracts + constantVariables + freeFunctions
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

  fun resolveNewExpression(parentNew: SolNewExpressionElement): Collection<PsiElement> {
    return parentNew.reference.multiResolve()
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
