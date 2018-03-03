package me.serce.solidity.ide.run.ui

import com.intellij.ide.util.AbstractTreeClassChooserDialog
import com.intellij.ide.util.TreeChooser
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import me.serce.solidity.ide.run.SearchUtils
import me.serce.solidity.lang.psi.SolContractDefinition
import java.util.*
import javax.swing.tree.DefaultMutableTreeNode

class TreeContractChooser(title: String, project: Project, scope: GlobalSearchScope, contractFilter: IContractFilter, initialClass: SolContractDefinition?) : AbstractTreeClassChooserDialog<SolContractDefinition>(title, project, scope, SolContractDefinition::class.java, createFilter(contractFilter), initialClass), TreeChooser<SolContractDefinition> {
  override fun getSelectedFromTreeUserObject(node: DefaultMutableTreeNode): SolContractDefinition? {
    return null // need implement?
  }

  override fun getClassesByName(
    name: String,
    checkBoxState: Boolean,
    pattern: String,
    searchScope: GlobalSearchScope
  ): List<SolContractDefinition> {
    val findContract = SearchUtils.findContract(name, project) ?: return Collections.emptyList()
    return Collections.singletonList(findContract)
  }
}

private fun createFilter(contractFilter: IContractFilter?): TreeChooser.Filter<SolContractDefinition>? {
  return if (contractFilter == null) {
    null
  } else {
    TreeChooser.Filter { element -> ReadAction.compute<Boolean, RuntimeException> { contractFilter.isAccepted(element) } }
  }
}
