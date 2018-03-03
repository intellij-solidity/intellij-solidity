package me.serce.solidity.ide.navigation

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.DefinitionsScopedSearch.SearchParameters
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.Query
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolInheritanceSpecifier
import java.util.*

private val MAX_IMPLEMENTATIONS = 250

class SolContractImplementationSearcher : QueryExecutorBase<PsiElement, SearchParameters>(true) {

  override fun processQuery(queryParameters: SearchParameters, consumer: Processor<PsiElement>) {
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
  while (implQueue.isNotEmpty() && implQueue.size < MAX_IMPLEMENTATIONS && implementations.size < MAX_IMPLEMENTATIONS) {
    val current = implQueue.poll()
    if (!implementations.add(current)) {
      continue
    }
    current.findImplementations()
      .filterQuery(Condition { !implementations.contains(it) })
      .forEach(implQueue::add)
  }
  return implementations
}

fun SolContractDefinition.findImplementations(): Query<SolContractDefinition> {
  return ReferencesSearch.search(this, this.useScope)
    .mapQuery { it.element.parent }
    .filterIsInstanceQuery<SolInheritanceSpecifier>()
    .mapQuery { it.parent }
    .filterIsInstanceQuery<SolContractDefinition>()
}
