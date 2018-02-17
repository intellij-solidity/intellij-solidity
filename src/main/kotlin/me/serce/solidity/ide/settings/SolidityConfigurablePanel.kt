package me.serce.solidity.ide.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import javax.swing.JPanel

class SolidityConfigurablePanel {
  private var myEvmPathField: TextFieldWithBrowseButton? = null
  private var myDbPathField: TextFieldWithBrowseButton? = null
  internal var myEvmPathPanel: JPanel? = null

  init {
    val descriptor = FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor()
    descriptor.title = "Solidity EVM Configuration"
    descriptor.description = "Select path to EthereumJ VM library"
    myEvmPathField!!.addBrowseFolderListener(descriptor.title, descriptor.description, null, descriptor)

    val descriptor2 = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    descriptor2.title = "Transaction Data Base"
    descriptor2.description = "Select path to EVM Data Base"
    myDbPathField!!.addBrowseFolderListener(descriptor2.title, descriptor2.description, null, descriptor2)
  }

  internal fun reset(settings: SoliditySettings) {
    val pathToEvm = settings.pathToEvm
    if (pathToEvm != null) {
      myEvmPathField!!.text = FileUtil.toSystemDependentName(pathToEvm)
    }
    val pathToDb = settings.pathToDb
    if (pathToDb != null) {
      myDbPathField!!.text = FileUtil.toSystemDependentName(pathToDb)
    }
  }

  internal fun apply(settings: SoliditySettings) {
    settings.pathToEvm = fromPath(myEvmPathField)
    settings.pathToDb = fromPath(myDbPathField)
  }

  private fun fromPath(textField: TextFieldWithBrowseButton?) = FileUtil.toSystemIndependentName(textField!!.text.trim())

  internal fun isModified(settings: SoliditySettings): Boolean {
    return fromPath(myEvmPathField) != settings.pathToEvm?.trim() || fromPath(myDbPathField) != settings.pathToDb?.trim()
  }
}
