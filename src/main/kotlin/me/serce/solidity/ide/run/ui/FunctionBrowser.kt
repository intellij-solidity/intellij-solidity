package me.serce.solidity.ide.run.ui

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.execution.configuration.BrowseModuleValueActionListener
import com.intellij.execution.ui.ConfigurationModuleSelector
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Condition
import com.intellij.ui.EditorTextField
import com.intellij.util.TextFieldCompletionProvider
import me.serce.solidity.ide.run.SearchUtils
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import javax.swing.JComponent

abstract class FunctionBrowser(project: Project) : BrowseModuleValueActionListener<JComponent>(project) {

  protected abstract val contractName: String
  protected abstract val moduleSelector: ConfigurationModuleSelector
  protected abstract fun getFilter(contract: SolContractDefinition?): Condition<SolFunctionDefinition>

  override fun showDialog(): String? {
    if (contractName.trim().isEmpty()) {
      Messages.showMessageDialog(field, "Set contract name first",
        "Cannot Browse Functions", Messages.getInformationIcon())
      return null
    }
    val contract = SearchUtils.findContract(contractName, project)
    if (contract == null) {
      Messages.showMessageDialog(field, "Class $contractName does not exist",
        "Cannot Browse Functions",
        Messages.getInformationIcon())
      return null
    }
    val dlg = FunctionListDialog(contract, getFilter(contract), field)
    if (dlg.showAndGet()) {
      val function = dlg.selected
      if (function != null) {
        return function.name
      }
    }
    return null
  }

  fun installCompletion(field: EditorTextField) {
    object : TextFieldCompletionProvider() {
      override fun addCompletionVariants(text: String, offset: Int, prefix: String, result: CompletionResultSet) {
        if (contractName.trim().isEmpty()) {
          return
        }
        val contract = SearchUtils.findContract(contractName, project) ?: return
        val filter = getFilter(contract)
        contract.functionDefinitionList
          .filter { filter.value(it) }
          .forEach { result.addElement(LookupElementBuilder.create(it.name!!)) }
      }
    }.apply(field)
  }

}
