package me.serce.solidity.ide.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import javax.swing.JButton
import javax.swing.JPanel

class SolidityConfigurablePanel {
  private lateinit var myEvmPathField: TextFieldWithBrowseButton
  private lateinit var myDbPathField: TextFieldWithBrowseButton
  private lateinit var downloadBtn: JButton
  internal lateinit var myEvmPathPanel: JPanel

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
  }

  internal fun reset(settings: SoliditySettings) {
    val pathToEvm = settings.pathToEvm
    if (pathToEvm != null) {
      myEvmPathField.text = FileUtil.toSystemDependentName(pathToEvm)
    }
    val pathToDb = settings.pathToDb
    if (pathToDb != null) {
      myDbPathField.text = FileUtil.toSystemDependentName(pathToDb)
    }
  }

  internal fun apply(settings: SoliditySettings) {
    val evmPath = fromPath(myEvmPathField)
    if (evmPath.isNotBlank() && !SoliditySettings.validateEvm(evmPath)) {
      throw ConfigurationException("Incorrect EVM path")
    }
    settings.pathToEvm = evmPath
    settings.pathToDb = fromPath(myDbPathField)
    ApplicationManager.getApplication().messageBus.syncPublisher(SoliditySettingsListener.TOPIC).settingsChanged()
  }

  private fun fromPath(textField: TextFieldWithBrowseButton?) = FileUtil.toSystemIndependentName(textField!!.text.trim())

  internal fun isModified(settings: SoliditySettings): Boolean {
    return fromPath(myEvmPathField) != settings.pathToEvm?.trim() || fromPath(myDbPathField) != settings.pathToDb?.trim()
  }
}
