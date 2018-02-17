package me.serce.solidity.ide.run

import com.intellij.openapi.project.Project
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.stubs.SolGotoClassIndex


object SearchUtils {
  fun findContract(contractName: String?, project: Project): SolContractDefinition? {
    val elements = StubIndex.getElements(SolGotoClassIndex.KEY, contractName!!, project, null, SolNamedElement::class.java)
    val find = elements.find { it is SolContractDefinition } ?: return null
    return find as SolContractDefinition?
  }

  val runnableFilter = { psiMethod : SolFunctionDefinition -> !psiMethod.isConstructor && psiMethod.parameters.isEmpty() }

}
