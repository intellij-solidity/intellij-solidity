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
          resolveUserDefinedValueType(element) +
          resolveAliases(element)
      }
      CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

  private fun resolveAliases(element: PsiElement): Set<SolNamedElement> {
    return resolveUsingImports(SolImportAlias::class.java, element, element.containingFile, true)
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


    // Retrieve all PSI elements with the name we're trying to lookup.
    val elements: Collection<SolNamedElement> = StubIndex.getElements( //
      SolNamedElementIndex.KEY, //
      elementName, //
      element.project, //
      null, //
      SolNamedElement::class.java //
    )

    val resolvedImportedFiles = collectImports(file)
    val insideImport = element.parentOfType<SolImportDirective>() != null
    val sameNameReferences = elements.filterIsInstance(target).filter {
      val containingFile = it.containingFile
      // During completion, IntelliJ copies PSI files, and therefore we need to ensure that we compare
      // files against its original file.
      val originalFile = file.originalFile
      // Below, either include
      containingFile == originalFile || resolvedImportedFiles.any { (insideImport || containingFile == it.file) && it.names.let {it.isEmpty() || it.any { it.name == elementName }}}


    }
    return sameNameReferences.toSet()
  }

  data class ImportRecord(val file: PsiFile, val names: List<SolNamedElement>)

  fun collectImports(file: PsiFile): Collection<ImportRecord> {
    return collectImports(file.childrenOfType<SolImportDirective>()).filter { it.file !== file }
  }

  /**
   * Collects imports of all declarations for a given file recursively.
   */
  private fun collectImports(imports: Collection<SolImportDirective>, visited: MutableSet<PsiFile> = hashSetOf()): Collection<ImportRecord> {
    if (!visited.add((imports.firstOrNull() ?: return emptyList()).containingFile)) {
      return emptySet()
    }

    val (resolvedImportedFiles, concreteResolvedImportedFiles) = imports.partition { it.importAliasedPairList.isEmpty() }.toList()
      .map {
        it.mapNotNull {
          val containingFile = it.importPath?.reference?.resolve()?.containingFile ?: return@mapNotNull null
          val aliases = it.importAliasedPairList
          val names = if (aliases.isNotEmpty()) {
            aliases.mapNotNull { it.importAlias } + aliases.mapNotNull { it.userDefinedTypeName.name?.let { tn -> containingFile.childrenOfType<SolContractDefinition>().find { it.name == tn } } }
          } else containingFile.childrenOfType<SolContractDefinition>().toList().flatMap { resolveContractMembers(it) + it }
          ImportRecord(containingFile, names.filterIsInstance<SolNamedElement>())
        }
      }

    val result = concreteResolvedImportedFiles + resolvedImportedFiles
    return result + result.map { collectImports(it.file.childrenOfType<SolImportDirective>(), visited) }.flatten()
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

  private fun resolveTypeNameStrict(element: SolReferenceElement) : Collection<SolNamedElement> {
    val names = resolveTypeName(element)
    return names.takeIf { it.size <= 1 } ?: resolveTypeNameUsingImports(element)
  }

  fun resolveModifier(modifier: SolModifierInvocationElement): List<SolModifierDefinition> = StubIndex.getElements(
    SolModifierIndex.KEY,
    modifier.firstChild.text,
    modifier.project,
    null,
    SolNamedElement::class.java
  ).filterIsInstance<SolModifierDefinition>()
    .toList()

  fun resolveVarLiteralReference(element: SolNamedElement): List<SolNamedElement> {
    return when {
        element.parent?.parent is SolFunctionCallExpression -> {
          val functionCall = element.findParentOrNull<SolFunctionCallElement>()!!
          val resolved = functionCall.reference?.multiResolve() ?: emptyList()
          if (resolved.isNotEmpty()) {
            resolved.filterIsInstance<SolNamedElement>()
          } else {
            resolveVarLiteral(element)
          }
        }
        element.parent is SolModifierInvocation -> {
          val modifierInvocation = element.parent as SolModifierInvocation
          fun resolveModifier() = modifierInvocation.reference?.multiResolve()?.filterIsInstance<SolNamedElement>()?.takeIf { it.isNotEmpty() }
          when {
            element.parent.parent is SolConstructorDefinition -> element.findContract()?.collectSupers?.filter { resolveTypeNameStrict(it).filterIsInstance<SolContractDefinition>().any { it.name == element.name } }?.takeIf { it.isNotEmpty() } ?: resolveModifier()
            else -> resolveModifier()
          } ?: emptyList()
        }
        else -> {
          resolveVarLiteral(element)
            .findBest {
              when (it) {
                is SolVariableDeclaration -> 1
                is SolParameterDef -> 10
                is SolStateVariableDeclaration -> 100
                else -> Int.MAX_VALUE
              }
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
        .toList().let {
          if (it.size <= 1 || it.any { it !is SolContractDefinition }) it
          // resolve by imports to distinguish elements with the same name
          else resolveTypeNameUsingImports(element).toList()
        }
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
    val isFunctionCall = element.nextSibling is SolFunctionInvocation

    return when (val memberName = element.identifier?.text) {
      null -> emptyList()
      else -> element.getMembers()
        .filter { it.getName() == memberName && if (isFunctionCall) it !is SolFunctionReference else it !is SolFunctionDefinition}
    }
  }

  fun resolveContractMembers(contract: SolContractDefinition, skipThis: Boolean = false): List<SolMember> {
    val members = if (!skipThis)
      contract.stateVariableDeclarationList as List<SolMember> + contract.functionDefinitionList + contract.functionDefinitionList.filter { it.visibility?.let { it == Visibility.PUBLIC || it == Visibility.EXTERNAL } ?: false }.map { SolFunctionReference(it) }  +
        contract.enumDefinitionList.map { SolEnum(it) } +
        contract.structDefinitionList.map { SolMemberConstructor(it) } + contract.eventDefinitionList.map { SolMemberConstructor(it) } + contract.errorDefinitionList.map { SolMemberConstructor(it) }
    else
      emptyList()
    return members + contract.supers
      .map { resolveTypeNameStrict(it).firstOrNull() }
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
          .map { resolveTypeNameStrict(it).firstOrNull() }
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
      is SolModifierDefinition -> {
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
      is SolUncheckedBlock -> {
        scope.statementList.asSequence()
          .map { lexicalDeclarations(visitedScopes, it, place) }
          .flatten()
      }

      is SolidityFile -> {
        RecursionManager.doPreventingRecursion(scope.name, true) {
          val scopeChildren = scope.children
          val contracts = scopeChildren.asSequence()
            .filterIsInstance<SolContractDefinition>()

          val constantVariables = scopeChildren.asSequence()
            .filterIsInstance<SolConstantVariable>()

          val freeFunctions = scopeChildren.asSequence()
            .filterIsInstance<SolFunctionDefinition>()

          val imports = scopeChildren.asSequence().filterIsInstance<SolImportDirective>()
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
          val identifier = it.identifier
          when {
            declarationList != null -> declarationList.declarationItemList.asSequence()
            typedDeclarationList != null -> typedDeclarationList.typedDeclarationItemList.asSequence()
            identifier != null -> listOf(it).asSequence()
            else -> emptySequence()
          }
        } ?: emptySequence()
      }
      is SolTryStatement -> {
        scope.parameterList?.parameterDefList?.asSequence() ?: emptySequence()
      }
      is SolCatchClause -> {
        scope.parameterList?.parameterDefList?.asSequence() ?: emptySequence()
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
  val parameterPairs = parseParameters()
  val paramMap = arguments.expressionList.firstOrNull()
  if (paramMap is SolMapExpression) {
    val callArguments = paramMap.mapExpressionClauseList.mapNotNull { Pair(it.identifier.text, it.expression?.type ?: return@mapNotNull null) }
    if (callArguments.size != parameterPairs.size) {
      return false
    }
    return !parameterPairs.sortedBy { it.first }.zip(callArguments.sortedBy { it.first })
          .any { (param, argument) ->
            param.first != argument.first || param.second != SolUnknown && !param.second.isAssignableFrom(argument.second)
          }
  } else {
    val callArgumentTypes = arguments.expressionList.map { it.type }
    val parameters = parameterPairs
      .map { it.second }
    val varargs = parameterPairs.find { it.first == SolInternalTypeFactory.varargsId }
    if (parameters.size != callArgumentTypes.size && varargs == null)
      return false
    return !((parameters.takeIf { varargs == null || callArgumentTypes.size <= parameters.size } ?: (parameters.toMutableList() + List(callArgumentTypes.size - parameters.size) { varargs!!.second })).zip(callArgumentTypes)
      .any { (paramType, argumentType) ->
        paramType != SolUnknown && !paramType.isAssignableFrom(argumentType)
      })
  }
}
