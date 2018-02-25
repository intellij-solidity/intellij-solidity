package me.serce.solidity.ide.run

import com.intellij.execution.CantRunException
import com.intellij.execution.testframework.SourceScope
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.search.GlobalSearchScope
import me.serce.solidity.ide.run.ui.IContractFilter
import me.serce.solidity.lang.psi.SolContractDefinition


open class ContractFilter(private val myScope: GlobalSearchScope) : IContractFilter.ContractFilterWithScope {
  class NoContractException : CantRunException("No contracts found")

  companion object {

    @Throws(NoContractException::class)
    fun create(sourceScope: SourceScope): ContractFilter {
      return ContractFilter(sourceScope.globalSearchScope)
    }
  }

  override fun isAccepted(contract: SolContractDefinition): Boolean {
    return ReadAction.compute<Boolean, RuntimeException> {
      contract.name != null
    }
  }

  override val scope: GlobalSearchScope
    get() = myScope

}
