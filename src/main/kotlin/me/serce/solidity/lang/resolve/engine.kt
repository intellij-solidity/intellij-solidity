package me.serce.solidity.lang.resolve

import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.util.*
import com.intellij.util.Processors
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolNewExpressionElement
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.resolve.ref.SolReference
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.lang.stubs.SolModifierIndex
import me.serce.solidity.lang.stubs.SolNamedElementIndex
import me.serce.solidity.lang.types.*
import me.serce.solidity.wrap

object SolResolver {
  fun resolveTypeNameUsingImports(element: PsiElement): Set<SolNamedElement> =
    CachedValuesManager.getCachedValue(element) {
      var result = if (element is SolFunctionCallElement) {
        resolveTypeWhenFunctionCallElement(element)
      } else {
        resolveTypeWhenFunctionCallElement(element) +
          resolveAliases(element) +
          resolveStruct(element)
      }
      if (result.any { it !is SolImportAlias }) {
        result = result.filter { it !is SolImportAlias }.toSet()
      }
      CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

  private fun resolveTypeWhenFunctionCallElement(element: PsiElement) =
    resolveError(element) +
      resolveContract(element) +
      resolveEvent(element) +
      resolveEnum(element) +
      resolveUserDefinedValueType(element) +
      resolveConstant(element)

  fun resolveAliases(element: PsiElement): Set<SolNamedElement> {
    return resolveUsingImports(SolImportAlias::class.java, element, element.containingFile)
  }

  private fun <T : SolNamedElement> resolveUsingImports(
    target: Class<T>,
    element: PsiElement,
    file: PsiFile,
  ): Set<T> {
    // If the elements has no name or text, we can't resolve it.
    val elementName = element.nameOrText ?: return emptySet()

    // Retrieve all PSI elements with the name we're trying to lookup.
    val elements: Collection<SolNamedElement> = StubIndex.getElements( //
      SolNamedElementIndex.KEY, //
      elementName, //
      element.project, //
      null, //
      SolNamedElement::class.java //
    )

    val resolvedImportedFiles = collectImports(file)

    val sameNameReferences = elements.filter {
      val containingFile = it.containingFile
      // During completion, IntelliJ copies PSI files, and therefore we need to ensure that we compare
      // files against its original file.
      val originalFile = file.originalFile
      // Below, either include
      containingFile == originalFile || resolvedImportedFiles.any { (containingFile == it.file) && it.names.let { it.isEmpty() || it.any { it.name == elementName } } }
    }

    val sameNameReferencesAndType = sameNameReferences.filterIsInstance(target)
    if (sameNameReferencesAndType.isNotEmpty()) {
      return sameNameReferencesAndType.toSet()
    } else if (sameNameReferences.filterIsInstance<SolImportAlias>().isNotEmpty()) {
      val aliasElement = sameNameReferences.filterIsInstance<SolImportAlias>().first()
      if (!isAliasOfFile(aliasElement)) {
        val elementFromAlias = (aliasElement.parent as SolImportAliasedPair).userDefinedTypeName
        val aliasFile =
          aliasElement.parentOfType<SolImportDirective>()?.importPath?.reference?.resolve()?.containingFile
        if (aliasFile != null) {
          return RecursionManager.doPreventingRecursion(aliasFile, true) {
            resolveUsingImports(target, elementFromAlias, aliasFile)
          } ?: emptySet()
        }
      }
    }

    return emptySet()
  }

  data class ImportRecord(val file: PsiFile, val names: List<SolNamedElement>)

  fun collectImports(file: PsiFile): Collection<ImportRecord> {
    return collectImports(file.childrenOfType<SolImportDirective>()).filter { it.file !== file }
  }

  private val exportElements = setOf(
    SolContractDefinition::class.java,
    SolConstantVariableDeclaration::class.java,
    SolEnumDefinition::class.java,
    SolErrorDefinition::class.java,
    SolStructDefinition::class.java,
  )

  fun collectUsedElements(o: SolImportDirective): List<String> {
    val containingFile = o.containingFile

    val importedNames = collectImportedNames(containingFile)

    val pathes = collectImports(o).map { it.file }
    val importScope = GlobalSearchScope.filesScope(o.project, pathes.map { it.virtualFile })

    val imported = pathes.flatMap {
      CachedValuesManager.getCachedValue(it) {
        val allKeys = HashSet<String>()
        val scope = GlobalSearchScope.fileScope(it)
        StubIndex.getInstance().processAllKeys(SolNamedElementIndex.KEY, Processors.cancelableCollectProcessor(allKeys), scope)
        CachedValueProvider.Result.create(allKeys.filter { StubIndex.getElements(SolNamedElementIndex.KEY, it, scope.project!!, scope, SolNamedElement::class.java).isNotEmpty() }.toSet(), PsiModificationTracker.MODIFICATION_COUNT)
      }
    }

    fun PsiElement.outerIdentifier() = (this as? SolUserDefinedTypeName)?.findIdentifiers()?.takeIf { it.size == 2 }?.firstOrNull()?.nameOrText ?: ""
    val targetNames = importedNames.flatMap {
      ((it.target.outerContract()?.let { listOf(it) } ?: emptyList()) + it.target + it.ref ).mapNotNull { it.name } + it.ref.outerIdentifier()
    }.toSet()
    val used = imported.intersect(targetNames)
      .filter {
        StubIndex.getElements(SolNamedElementIndex.KEY, it, o.project, importScope, SolNamedElement::class.java)
          .all { e -> exportElements.any { it.isAssignableFrom(e.javaClass) } }
      }

    val specificNames = o.importAliasedPairList.flatMap { ((getSolType(it.userDefinedTypeName) as? SolContract)?.let { resolveContractNestedNames(it.ref) } ?: emptyList()) +  it.userDefinedTypeName }.mapNotNull { it.name }.toSet()
    return used.takeIf { specificNames.isEmpty() } ?: used.filter { it in specificNames }
  }

  fun collectImportedNames(root: PsiFile): Set<ImportedName> {
    return CachedValuesManager.getCachedValue(root) {
      val result = root.descendants().filter { it is SolUserDefinedTypeName && it.parentOfType<SolImportDirective>() == null || it is SolVarLiteral }
                  .flatMap { ref -> (ref.reference as? SolReference)?.multiResolve()?.mapNotNull { Pair(ref as? SolNamedElement ?: return@mapNotNull null, it as? SolNamedElement ?: return@mapNotNull null) } ?: emptyList() }
                  .mapNotNull { (ref, it) ->
                    ImportedName(ref, when {
                        it.isBuiltin() -> null
                        it is SolConstructorDefinition -> it.findContract()
                        it.containingFile != root -> it
                        else -> (it.parent as? SolImportAliasedPair)?.userDefinedTypeName
                    } ?: return@mapNotNull null)
                  }.toSet()
      CachedValueProvider.Result.create(result, root)
    }
  }

  data class ImportedName(val ref: SolNamedElement, val target: SolNamedElement)


  fun collectImports(import: SolImportDirective): Collection<ImportRecord> {
    return CachedValuesManager.getCachedValue(import) {
      val result = collectImports(listOf(import))
      CachedValueProvider.Result.create(result, result.map { it.file } + import)
    }
  }

  fun collectImports(imports: Collection<SolImportDirective>): Collection<ImportRecord> {
    return RecursionManager.doPreventingRecursion(imports, true) {
      val visited: MutableSet<PsiFile> = hashSetOf()
      collectImports(imports, visited)
    } ?: emptySet()
  }

  /**
   * Collects imports of all declarations for a given file recursively.
   */
  private fun collectImports(
    imports: Collection<SolImportDirective>,
    visited: MutableSet<PsiFile>
  ): Collection<ImportRecord> {
    if (!visited.add((imports.firstOrNull() ?: return emptyList()).containingFile)) {
      return emptySet()
    }

    val (resolvedImportedFiles, concreteResolvedImportedFiles) = imports.partition { it.importAliasedPairList.isEmpty() }
      .toList()
      .map {
        it.mapNotNull { import ->
          val containingFile = import.importPath?.reference?.resolve()?.containingFile ?: return@mapNotNull null
          val aliases = import.importAliasedPairList
          val names = if (aliases.isNotEmpty()) {
            aliases.mapNotNull { importAliasPair -> importAliasPair.importAlias } + aliases.mapNotNull { importAliasPair ->
              importAliasPair.userDefinedTypeName.name?.let { tn ->
                containingFile.childrenOfType<SolContractDefinition>().find { contract -> contract.name == tn }
              }
            }
          } else containingFile.childrenOfType<SolCallableElement>().toList()
            .flatMap { element -> (if (element is SolContractDefinition) resolveContractMembers(element) else emptyList()) + it }
          ImportRecord(containingFile, names.filterIsInstance<SolNamedElement>())
        }
      }

    val result = concreteResolvedImportedFiles + resolvedImportedFiles
    return result + result.map { record ->
      val contractsOrLibsInFile = record.file.childrenOfType<SolContractDefinition>()
      //support only with one contract or lib by file
      if (contractsOrLibsInFile.size == 1 && isExternalLibrary(contractsOrLibsInFile.first())) {
        emptyList()
      } else {
        collectImports(record.file.childrenOfType<SolImportDirective>(), visited)
      }
    }.flatten()
  }

  private fun isExternalLibrary(element: SolContractDefinition): Boolean {
    return (element.contractType == ContractType.LIBRARY
      && element.functionDefinitionList.all { function ->
        function.visibility == Visibility.EXTERNAL
          || function.visibility == Visibility.PUBLIC
    })
  }

  //collect all SolContractDefinition recursively from imports
  fun collectContracts(import: SolImportDirective): Collection<SolContractDefinition> {
    return collectImports(import).flatMap { it.file.childrenOfType<SolContractDefinition>() }
  }

  fun collectChildrenOfFile(import: SolImportDirective): Collection<SolCallable> {
    return collectImports(import).flatMap { it.file.childrenOfType<SolCallableElement>() }
  }

  fun collectImportDirective(import: SolImportDirective): Collection<SolImportDirective> {
    return collectImports(import).flatMap { it.file.childrenOfType<SolImportDirective>() }
  }

  private fun resolveContract(element: PsiElement): Set<SolContractDefinition> =
    resolveUsingImports(SolContractDefinition::class.java, element, element.containingFile)
  private fun resolveEnum(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolEnumDefinition>(element) { it.enumDefinitionList } + resolveUsingImports(SolEnumDefinition::class.java, element, element.containingFile)

  private fun resolveStruct(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolStructDefinition>(element) { it.structDefinitionList } + resolveUsingImports(SolStructDefinition::class.java, element, element.containingFile)

  private fun resolveUserDefinedValueType(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolUserDefinedValueTypeDefinition>(
      element,
      { it.userDefinedValueTypeDefinitionList }) + resolveUsingImports(SolUserDefinedValueTypeDefinition::class.java, element, element.containingFile)

  private fun resolveConstant(element: PsiElement): Set<SolNamedElement> =
    resolveUsingImports(SolConstantVariable::class.java, element, element.containingFile)
  private fun resolveEvent(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolEventDefinition>(element) { it.eventDefinitionList } + resolveUsingImports(SolEventDefinition::class.java, element, element.containingFile)

  private fun resolveError(element: PsiElement): Set<SolNamedElement> =
    resolveInnerType<SolErrorDefinition>(element) { it.errorDefinitionList } + resolveUsingImports(SolErrorDefinition::class.java, element, element.containingFile)

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
        names.size > 2 -> {
          val indexNameWithoutAliases = names.indexOf(names.find { name ->
            val resolved = resolveTypeNameUsingImports(name).first()
            resolved !is SolImportAlias || !isAliasOfFile(resolved)
          })
          if (names.size - indexNameWithoutAliases == 2) {
            resolveTypeNameUsingImports(names[indexNameWithoutAliases])
              .filterIsInstance<SolContractDefinition>()
              .firstOrNull()
              ?.let { resolveInnerType(it, names[indexNameWithoutAliases + 1].nameOrText!!, f) } ?: emptySet()
          } else if (names.size - indexNameWithoutAliases == 1) {
            element.parentOfType<SolContractDefinition>()
              ?.let {
                names[indexNameWithoutAliases].nameOrText?.let { nameOrText ->
                  resolveInnerType(it, nameOrText, f)
                }
              } ?: emptySet()
          } else {
            emptySet()
          }
        }
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
      else -> {
        var elementToSearch = element
        val importAlias = resolveAlias(element)
        if (importAlias != null) {
          elementToSearch = getElementFromAlias(element, importAlias)
          //if it has the same name, then we can only link to the alias of the contract
          if (elementToSearch.name == element.name) {
            return listOf(elementToSearch)
          }
        }

        lexicalDeclarations(element)
          .filter { it.name == elementToSearch.name }
          .toList().let {
            if (it.size <= 1 || it.any { it !is SolContractDefinition }) it
            // resolve by imports to distinguish elements with the same name
            else resolveTypeNameUsingImports(element).toList()
          }
      }
    }
  }

  fun resolveAlias(element: SolNamedElement): SolImportDirective? {
    return element.containingFile.childrenOfType<SolImportDirective>().find {
      it.importAlias?.name == element.name ||
              it.importAliasedPairList.any { importAliasedPair ->
                importAliasedPair.importAlias?.name == element.name
              }
    }
  }

  fun isAliasOfFile(import: SolImportDirective ): Boolean {
    return when (import.importAlias) {
      null -> false
      else -> true
    }
  }

  fun isAliasOfFile(alias :SolImportAlias): Boolean {
    return when (alias.parent) {
      is SolImportAliasedPair -> false
      else -> true
    }
  }

  //return the right element to search
  private fun getElementFromAlias(element: SolNamedElement, import: SolImportDirective): SolNamedElement {
    if (import.importAlias?.name == element.name) {
        //match import * as A from "path"
        //match import "path" as A
        return import.importAlias!!
    } else {
      //match import {a as A} from "path"
      import.importAliasedPairList.forEach { importAliasedPair ->
        if (importAliasedPair.importAlias?.name == element.name) {
          return importAliasedPair.userDefinedTypeName
        }
      }
    }

    return element
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

  fun resolveMemberAccessWithAliases(
    element: PsiElement,
    elementToFind: SolMemberAccessExpression
  ): List<SolNamedElement> {
    return resolveTypeNameUsingImports(element).let { result ->
      if (result.isEmpty()) {
        emptyList()
      } else if (result.any { it !is SolImportAlias }) {
        resolveMemberAccessWithChildElement(result, elementToFind)
      } else if (element.parent is SolMemberAccessExpression) {
        resolveMemberAccessWithAliases(element.parent, elementToFind)
      } else {
        emptyList()
      }
    }
  }

  private fun resolveMemberAccessWithChildElement(
    elements: Set<SolNamedElement>,
    elementToFind: SolMemberAccessExpression
  ): List<SolNamedElement> {
    val childrenElements = elements.filter { it !is SolImportAlias }.flatMap {
      it.childrenOfType<SolNamedElement>()
    }.toSet()
    if (childrenElements.isEmpty()) {
      return emptyList()
    }
    val childrenElementsFiltered = childrenElements.filter { childElement ->
      childElement.name == elementToFind.identifier!!.text
    }
    return childrenElementsFiltered.ifEmpty {
      resolveMemberAccessWithChildElement(childrenElements, elementToFind)
    }
  }

  fun resolveContractMembers(contract: SolContractDefinition, skipThis: Boolean = false): List<SolMember> {
    val members = if (!skipThis)
      contract.stateVariableDeclarationList as List<SolMember> + contract.functionDefinitionList +
        contract.functionDefinitionList.filter { it.visibility?.let { it == Visibility.PUBLIC || it == Visibility.EXTERNAL || it == Visibility.INTERNAL && contract.contractType == ContractType.LIBRARY } ?: false }.map { SolFunctionReference(it) }  +
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
          .flatten() + scope.structDefinitionList + scope.eventDefinitionList + scope.errorDefinitionList + scope.userDefinedValueTypeDefinitionList
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

          val userDefinedTypes = scopeChildren.asSequence()
            .filterIsInstance<SolUserDefinedValueTypeDefinition>()

          val enums = scopeChildren.asSequence()
            .filterIsInstance<SolEnumDefinition>()

          val imports = scopeChildren.asSequence().filterIsInstance<SolImportDirective>()
            .mapNotNull {  it.importPath?.reference?.resolve()?.containingFile }
            .map { lexicalDeclarations(visitedScopes, it, place) }
            .flatten()
          imports + contracts + constantVariables + freeFunctions + userDefinedTypes + enums
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

  private fun resolveContractNestedNames(contract: SolContractDefinition, skipThis: Boolean = false): List<SolNamedElement> {
    val members = if (!skipThis) {
      contract.structDefinitionList + contract.enumDefinitionList + contract.errorDefinitionList + contract.userDefinedValueTypeDefinitionList
    } else emptyList()
    return members + contract.supers
      .map { resolveTypeName(it).firstOrNull() }
      .filterIsInstance<SolContractDefinition>()
      .flatMap { resolveContractNestedNames(it) }
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
