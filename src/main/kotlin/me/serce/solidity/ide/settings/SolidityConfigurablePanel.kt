package me.serce.solidity.ide.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import me.serce.solidity.ide.interop.Sol2JavaGenerationStyle
import me.serce.solidity.ide.run.hasJavaSupport
import java.awt.Component
import javax.swing.*

class SolidityConfigurablePanel {
  internal lateinit var myEvmPathPanel: JPanel
  private lateinit var myEvmPathField: TextFieldWithBrowseButton
  private lateinit var myDbPathField: TextFieldWithBrowseButton
  private lateinit var downloadBtn: JButton

  private lateinit var checkboxPanel: JPanel
  private lateinit var useSolcJ: JCheckBox
  private lateinit var javaInteropPanel: JPanel
  private lateinit var generateJavaStubs: JCheckBox
  private lateinit var dependecyAutoRefresh: JCheckBox
  private lateinit var web3jBtn: JRadioButton
  private lateinit var ethJNativeBtn: JRadioButton
  private lateinit var basePackageField: JTextField

  private lateinit var warningPanel: JPanel
  private lateinit var warningLabel: JLabel

  private val noJavaWarning = "Current IDE platform does not support running Solidity"

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
        updateControlAvailability()
        if (myEvmPathField.textField.text.isNotEmpty()) useSolcJ.isSelected = true
      }
    })
    generateJavaStubs.addActionListener {
      updateInteropControlsAvailability()
    }
    if (!hasJavaSupport) {
      warningLabel.icon = AllIcons.General.BalloonWarning
      warningLabel.text = noJavaWarning
    } else {
      warningLabel.isVisible = false
    }
  }

  private fun updateInteropControlsAvailability() {
    javaInteropPanel.setAll({ it.isEnabled = generateJavaStubs.isSelected }, except = generateJavaStubs)
  }

  private fun updateControlAvailability() {
    val enabled = myEvmPathField.textField.text.isNotEmpty()
    checkboxPanel.setAll({ it.isEnabled = enabled })
    if (!hasJavaSupport) {
      useSolcJ.isEnabled = false
      useSolcJ.isSelected = false
    }
  }

  private fun JPanel.setAll(action: (Component) -> Unit, vararg except: Component) {
    this.components.asSequence()
      .filterNot { except.contains(it) }
      .forEach {
        (it as? JPanel)?.setAll(action, *except)
        action(it)
      }
  }

  internal fun reset(settings: SoliditySettings) {
    myEvmPathField.text = FileUtil.toSystemDependentName(settings.pathToEvm)
    myDbPathField.text = FileUtil.toSystemDependentName(settings.pathToDb)
    useSolcJ.isSelected = settings.useSolcJ
    generateJavaStubs.isSelected = settings.generateJavaStubs
    dependecyAutoRefresh.isSelected = settings.dependenciesAutoRefresh
    basePackageField.text = settings.basePackage
    when (settings.genStyle) {
      Sol2JavaGenerationStyle.WEB3J -> web3jBtn.isSelected = true
      Sol2JavaGenerationStyle.ETHJ -> ethJNativeBtn.isSelected = true
    }
    updateControlAvailability()
    updateInteropControlsAvailability()
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
    settings.basePackage = basePackageField.text
    settings.genStyle = generationStyle()
    ApplicationManager.getApplication().messageBus.syncPublisher(SoliditySettingsListener.TOPIC).settingsChanged()
  }

  private fun generationStyle(): Sol2JavaGenerationStyle = when {
    web3jBtn.isSelected -> Sol2JavaGenerationStyle.WEB3J
    ethJNativeBtn.isSelected -> Sol2JavaGenerationStyle.ETHJ
    else -> throw IllegalStateException("Incorrect gen style option")
  }

  private fun fromPath(textField: TextFieldWithBrowseButton) = FileUtil.toSystemIndependentName(textField.text.trim())

  internal fun isModified(settings: SoliditySettings): Boolean =
    fromPath(myEvmPathField) != settings.pathToEvm.trim() ||
      fromPath(myDbPathField) != settings.pathToDb.trim() ||
      useSolcJ.isSelected != settings.useSolcJ ||
      generateJavaStubs.isSelected != settings.generateJavaStubs ||
      basePackageField.text != settings.basePackage ||
      generationStyle() != settings.genStyle
}
