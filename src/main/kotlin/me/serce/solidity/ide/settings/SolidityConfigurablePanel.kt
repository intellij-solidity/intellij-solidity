package me.serce.solidity.ide.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JPanel

class SolidityConfigurablePanel {
  internal lateinit var myEvmPathPanel: JPanel
  private lateinit var myEvmPathField: TextFieldWithBrowseButton
  private lateinit var myDbPathField: TextFieldWithBrowseButton
  private lateinit var downloadBtn: JButton

  private lateinit var checkboxPanel: JPanel
  private lateinit var useSolcJ: JCheckBox
  private lateinit var generateJavaStubs: JCheckBox
  private lateinit var dependecyAutoRefresh: JCheckBox

  init {
    val descriptor = FileChooserDescriptor(false, true, true, true, false, false)
    descriptor.title = "Solidity EVM Configuration"
    descriptor.description = "Select path to EthereumJ VM library"
    myEvmPathField.addBrowseFolderListener(descriptor.title, descriptor.description, null, descriptor)

    val descriptor2 = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    descriptor2.title = "Transaction Data Base"
    descriptor2.description = "Select path to EVM Data Base"
    myDbPathField.addBrowseFolderListener(descriptor2.title, descriptor2.description, null, descriptor2)

    downloadBtn.addActionListener {
      val dir = EvmDownloader.download(myEvmPathPanel)
      if (dir.isNotEmpty()) {
        myEvmPathField.text = dir
      }
    }
    myEvmPathField.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: javax.swing.event.DocumentEvent?) {
        updateCheckboxes()
        if (myEvmPathField.textField.text.isNotEmpty()) useSolcJ.isSelected = true
      }
    })
  }

  private fun updateCheckboxes() {
    val enabled = myEvmPathField.textField.text.isNotEmpty()
    checkboxPanel.components.forEach { it.isEnabled = enabled }
  }

  internal fun reset(settings: SoliditySettings) {
    myEvmPathField.text = FileUtil.toSystemDependentName(settings.pathToEvm)
    myDbPathField.text = FileUtil.toSystemDependentName(settings.pathToDb)
    useSolcJ.isSelected = settings.useSolcJ
    generateJavaStubs.isSelected = settings.generateJavaStubs
    dependecyAutoRefresh.isSelected = settings.dependenciesAutoRefresh
    updateCheckboxes()
  }

  internal fun apply(settings: SoliditySettings) {
    val evmPath = fromPath(myEvmPathField)
    if (evmPath.isNotBlank() && !SoliditySettings.validateEvm(evmPath)) {
      throw ConfigurationException("Incorrect EVM path")
    }
    settings.pathToEvm = evmPath
    settings.pathToDb = fromPath(myDbPathField)
    settings.useSolcJ = useSolcJ.isSelected
    settings.generateJavaStubs = generateJavaStubs.isSelected
    ApplicationManager.getApplication().messageBus.syncPublisher(SoliditySettingsListener.TOPIC).settingsChanged()
  }

  private fun fromPath(textField: TextFieldWithBrowseButton) = FileUtil.toSystemIndependentName(textField.text.trim())

  internal fun isModified(settings: SoliditySettings): Boolean {
    return fromPath(myEvmPathField) != settings.pathToEvm.trim() ||
      fromPath(myDbPathField) != settings.pathToDb.trim() ||
      useSolcJ.isSelected != settings.useSolcJ ||
      generateJavaStubs.isSelected != settings.generateJavaStubs
  }
}
