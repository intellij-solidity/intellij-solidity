package me.serce.solidity.ide.navigation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ex.ApplicationEx
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope.FilesScope
import com.intellij.psi.search.GlobalSearchScope.allScope
import com.intellij.psi.search.searches.DefinitionsScopedSearch.SearchParameters
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.childrenOfType
import com.intellij.util.ArrayQuery
import com.intellij.util.CollectionQuery
import com.intellij.util.EmptyQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolImportAliasedPair
import me.serce.solidity.lang.psi.SolInheritanceSpecifier
import java.util.*

private const val MAX_IMPLEMENTATIONS = 250

class SolContractImplementationSearcher : QueryExecutorBase<PsiElement, SearchParameters>(true) {

  override fun processQuery(queryParameters: SearchParameters, consumer: Processor<in PsiElement>) {
    val contract = queryParameters.element
    if (contract !is SolContractDefinition) {
      return
    }
    val implementations = contract.findAllImplementations()
    implementations.forEach { consumer.process(it) }
  }
}

fun SolContractDefinition.findAllImplementations(): HashSet<SolContractDefinition> {
  val implementations = HashSet<SolContractDefinition>()
  val implQueue = ArrayDeque<SolContractDefinition>(MAX_IMPLEMENTATIONS)
  implQueue.add(this)
  // Run the implementation resolution under an empty progress to avoid the noisy
  // "Must be executed under progress indicator" error, see https://github.com/intellij-solidity/intellij-solidity/issues/295
  // TODO: would it be worth using a real progress here?
  // TODO: would resolution from EDT only be called in tests? If not, it might trigger a warning again. If yes, then
  //    need to find a way to run tests from a non dispatcher thread.
  val application = ApplicationManager.getApplication() as ApplicationEx
  if (application.isDispatchThread) {
    findAllImplementationsInAction(implQueue, implementations)
  } else {
    ProgressIndicatorUtils.runInReadActionWithWriteActionPriority({
      findAllImplementationsInAction(implQueue, implementations)
    }, EmptyProgressIndicator())
  }
  implementations.remove(this)
  return implementations
}

private fun findAllImplementationsInAction(
  implQueue: ArrayDeque<SolContractDefinition>,
  implementations: HashSet<SolContractDefinition>
) {
  while (implQueue.isNotEmpty() && implQueue.size < MAX_IMPLEMENTATIONS && implementations.size < MAX_IMPLEMENTATIONS) {
    val current = implQueue.poll()
    if (!implementations.add(current)) {
      continue
    }
    current.findImplementations()
      .filterQuery(Condition { !implementations.contains(it) })
      .forEach(Processor { implQueue.add(it) })
  }
}

fun SolContractDefinition.findImplementations(): Query<SolContractDefinition> {
  val virtualFile = containingFile.virtualFile
  val resolveScope = when {
    // When the file for which we're performing search for usages is a part of the sources/libs in the current project
    virtualFile != null && resolveScope.contains(virtualFile) -> resolveScope
    // However, if the project doesn't store solidity files in its source folder, we perform a global search.
    else -> allScope(project)
  }
  val solOnlyScope = useScope.intersectWith(FilesScope.getScopeRestrictedByFileTypes(resolveScope, SolidityFileType))
  // Reference search can find two types of references:
  return ReferencesSearch.search(this, solOnlyScope)
    .flatMapping {
      when (val element = it.element.parent) {
        // 1. A contract is directly referenced in the inheritance specifier.
        //    In this case, we simply include the contract definition in the list of results.
        is SolInheritanceSpecifier -> {
          when (val parent = element.parent) {
            is SolContractDefinition -> ArrayQuery(parent)
            else -> EmptyQuery()
          }
        }
        // 2. A contract is referenced in the import alias.
        //    Because we want all referenced through aliases to be resolvable directly, we can't
        //    rely on the reference search to find the references to a particular alias. So, instead
        //    we search through all contract definition in the file containing the alias.
        // TODO: consider whether we could to either allow resolving references to aliases which
        //   should also allow renaming. Or, alternatively, add a separate index for faster search.
        is SolImportAliasedPair -> {
          val containingFile = element.containingFile
          val alias = element.importAlias
          // TODO: get inheritanceSpecifierList in order to consider constructor calls as well.
          val results = containingFile
            .childrenOfType<SolContractDefinition>()
            .filter {
              it.inheritanceSpecifierList.map { it.userDefinedTypeName.name }.contains(alias?.name)
            }
          CollectionQuery(results)
        }
        else -> EmptyQuery()
      }
    }
}
