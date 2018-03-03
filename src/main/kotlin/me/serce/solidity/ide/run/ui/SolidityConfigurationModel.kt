package me.serce.solidity.ide.run.ui

import com.intellij.execution.JavaExecutionUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import me.serce.solidity.ide.run.SearchUtils
import me.serce.solidity.ide.run.SolidityRunConfig
import me.serce.solidity.lang.psi.SolContractDefinition
import javax.swing.text.BadLocationException
import javax.swing.text.PlainDocument

class SolidityConfigurationModel(private val myProject: Project) {

  private var myListener: SolidityConfigurableEditorPanel? = null
  private val myContractDocuments = arrayOfNulls<Any>(2)

  fun setListener(listener: SolidityConfigurableEditorPanel) {
    myListener = listener
  }

  fun setContractDocument(i: Int, doc: Any) {
    myContractDocuments[i] = doc
  }

  fun apply(configuration: SolidityRunConfig) {
    val shouldUpdateName = configuration.isGeneratedName
    applyTo(configuration.getPersistentData())
    if (shouldUpdateName && !JavaExecutionUtil.isNewName(configuration.name)) {
      configuration.setGeneratedName()
    }
  }

  private fun applyTo(data: SolidityRunConfig.Data) {
    val contractName = getContractTextValue(CONTRACT)
    try {
      data.functionName = getContractTextValue(FUNCTION)
      val contract = if (!myProject.isDefault && !StringUtil.isEmptyOrSpaces(contractName)) findPsiContract(contractName, myProject) else null
      if (contract != null && contract.isValid) {
        data.setContract(contract)
      } else {
        data.contractName = contractName
      }
    } catch (e: ProcessCanceledException) {
      data.contractName = contractName
    } catch (e: IndexNotReadyException) {
      data.contractName = contractName
    }
  }

  private fun findPsiContract(contractName: String, myProject: Project): SolContractDefinition? {
    return SearchUtils.findContract(contractName, myProject)
  }

  private fun getContractTextValue(index: Int): String {
    return getDocumentText(index, myContractDocuments)
  }

  fun reset(configuration: SolidityRunConfig) {
    val data = configuration.getPersistentData()
    setContractTextValue(CONTRACT, data.getGetContractName())
    setContractTextValue(FUNCTION, data.getGetFunctionName())
  }

  private fun setContractTextValue(index: Int, text: String) {
    setDocumentText(index, text, myContractDocuments)
  }

  private fun setDocumentText(index: Int, text: String, documents: Array<Any?>) {
    val document = documents[index]
    if (document is PlainDocument) {
      try {
        document.remove(0, document.length)
        document.insertString(0, text, null)
      } catch (e: BadLocationException) {
        throw RuntimeException(e)
      }
    } else {
      WriteCommandAction.runWriteCommandAction(myProject) { (document as Document).replaceString(0, document.textLength, text) }
    }
  }

  companion object {
    const val CONTRACT = 0
    const val FUNCTION = 1

    private fun getDocumentText(index: Int, documents: Array<Any?>): String {
      val document = documents[index]
      if (document is PlainDocument) {
        try {
          return document.getText(0, document.length)
        } catch (e: BadLocationException) {
          throw RuntimeException(e)
        }
      }
      return (document as Document).text
    }
  }
}
