package me.serce.solidity.ide.run

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.stubs.SolGotoClassIndex


object SearchUtils {
  fun findContract(contractName: String, project: Project, mod: Module? = null): SolContractDefinition? {
    val elements = StubIndex.getElements(SolGotoClassIndex.KEY, contractName, project, if (mod != null)  GlobalSearchScope.moduleScope(mod) else null, SolNamedElement::class.java)
    return elements.firstOrNull { it is SolContractDefinition && (mod == null || mod.moduleScope.contains(it.containingFile.virtualFile)) } as? SolContractDefinition
  }

  val runnableFilter = { psiMethod : SolFunctionDefinition -> !psiMethod.isConstructor /*&& psiMethod.parameters.isEmpty()*/ }

}
