package me.serce.solidity.ide.run.ui

import com.intellij.execution.configuration.BrowseModuleValueActionListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.psi.search.GlobalSearchScope
import me.serce.solidity.lang.psi.SolContractDefinition
import javax.swing.JComponent


abstract class ContractBrowser(project: Project, private val myTitle: String) : BrowseModuleValueActionListener<JComponent>(project) {

  @Throws(NoFilterException::class)
  protected abstract fun filter(): IContractFilter.ContractFilterWithScope

  override fun showDialog(): String? {
    val contractFilter: IContractFilter.ContractFilterWithScope
    try {
      contractFilter = filter()
    } catch (e: NoFilterException) {
      val info = e.messageInfo
      info.showNow()
      return null
    }

    val dialog = createContractChooser(contractFilter)
    configureDialog(dialog)
    dialog.showDialog()
    val psiContract = dialog.selected ?: return null
    return psiContract.name
  }

  protected open fun createContractChooser(contractFilter: IContractFilter.ContractFilterWithScope): TreeContractChooser {
    return TreeContractChooser(myTitle, project, contractFilter.scope, contractFilter, null)
  }

  private fun configureDialog(dialog: TreeContractChooser) {
    val className = text
    val psiContract = findContract(className) ?: return
    val directory = psiContract.containingFile.containingDirectory
    if (directory != null) dialog.selectDirectory(directory)
    dialog.select(psiContract)
  }

  protected abstract fun findContract(className: String): SolContractDefinition?

  class NoFilterException(val messageInfo: MessagesEx.MessageInfo) : Exception(messageInfo.message)
}

interface IContractFilter {

  fun isAccepted(aClass: SolContractDefinition): Boolean

  interface ContractFilterWithScope : IContractFilter {
    val scope: GlobalSearchScope
  }
}
