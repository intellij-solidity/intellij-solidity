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
import me.serce.solidity.ide.run.compile.Solc
import me.serce.solidity.ide.run.hasJavaSupport
import java.awt.Component
import javax.swing.*
import javax.swing.event.DocumentEvent

class SolidityConfigurablePanel {
  internal lateinit var mainPanel: JPanel
  private lateinit var compilePanel: JPanel
  private lateinit var evmPath: TextFieldWithBrowseButton
  private lateinit var evmDbPath: TextFieldWithBrowseButton
  private lateinit var downloadBtn: JButton
  private lateinit var useSolcEthereum: JCheckBox
  private lateinit var standaloneSolc: TextFieldWithBrowseButton

  private lateinit var solcVersion: JLabel

  private lateinit var useSolcJ: JCheckBox
  private lateinit var javaInteropPanel: JPanel
  private lateinit var generateJavaStubs: JCheckBox
  private lateinit var dependecyAutoRefresh: JCheckBox
  private lateinit var web3jBtn: JRadioButton
  private lateinit var ethJNativeBtn: JRadioButton
  private lateinit var basePackageField: JTextField
  private lateinit var genOutputPath: JTextField

  private lateinit var warningLabel: JLabel

  private val noJavaWarning = "Current IDE platform does not support running Solidity"

  init {
    val ethDescriptor = FileChooserDescriptor(false, true, true, true, false, false)
    ethDescriptor.title = "Solidity EVM Configuration"
    ethDescriptor.description = "Select path to EthereumJ VM library"
    evmPath.addBrowseFolderListener(ethDescriptor.title, ethDescriptor.description, null, ethDescriptor)

    val ethDbDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    ethDbDescriptor.title = "Transaction Data Base"
    ethDbDescriptor.description = "Select path to EVM Data Base"
    evmDbPath.addBrowseFolderListener(ethDbDescriptor.title, ethDbDescriptor.description, null, ethDbDescriptor)

    useSolcJ.addActionListener {
      updateCompileAvailablility()
    }

    downloadBtn.addActionListener {
      val dir = EvmDownloader.download(mainPanel)
      if (dir.isNotEmpty()) {
        evmPath.text = dir
      }
    }
    evmPath.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: javax.swing.event.DocumentEvent?) {
        updateSolcControlAvailablility()
      }
    })

    val solcDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    solcDescriptor.title = "Standalone Solc installation"
    solcDescriptor.description = "Select path to Solidity compiler"
    standaloneSolc.addBrowseFolderListener(solcDescriptor.title, solcDescriptor.description, null, solcDescriptor)

    standaloneSolc.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent?) {
        updateSolcControlAvailablility()
      }
    })

    useSolcEthereum.addActionListener {
      updateSolcControlAvailablility()
    }

    generateJavaStubs.addActionListener {
      updateInteropControlsAvailability()
    }
    if (!hasJavaSupport) {
      warningLabel.icon = AllIcons.General.BalloonWarning
      warningLabel.text = noJavaWarning
      useSolcJ.isVisible = false
    } else {
      warningLabel.isVisible = false
    }
  }

  private fun updateCompileAvailablility() {
    val enabled = useSolcJ.isSelected
    compilePanel.setAll({ it.isEnabled = enabled }, useSolcJ)
  }

  private fun updateInteropControlsAvailability() {
    val enabled = generateJavaStubs.isSelected
    javaInteropPanel.setAll({ it.isEnabled = enabled }, generateJavaStubs)
  }

  private fun updateSolcControlAvailablility() {
    standaloneSolc.isEnabled = !useSolcEthereum.isSelected
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
    evmPath.text = FileUtil.toSystemDependentName(settings.pathToEvm)
    evmDbPath.text = FileUtil.toSystemDependentName(settings.pathToDb)
    standaloneSolc.text = FileUtil.toSystemDependentName(settings.solcPath)
    useSolcEthereum.isSelected = settings.useSolcEthereum
    useSolcJ.isSelected = settings.useSolcJ
    generateJavaStubs.isSelected = settings.generateJavaStubs
    dependecyAutoRefresh.isSelected = settings.dependenciesAutoRefresh
    basePackageField.text = settings.basePackage
    when (settings.genStyle) {
      Sol2JavaGenerationStyle.WEB3J -> web3jBtn.isSelected = true
      Sol2JavaGenerationStyle.ETHJ -> ethJNativeBtn.isSelected = true
    }
    genOutputPath.text = FileUtil.toSystemDependentName(settings.genOutputPath)
    updateCompileAvailablility()
    updateInteropControlsAvailability()
    solcVersion.text = Solc.getVersion()
  }

  internal fun apply(settings: SoliditySettings) {
    val evmPath = fromPath(evmPath)
    if (evmPath.isNotBlank() && !SoliditySettings.validateEvm(evmPath)) {
      throw ConfigurationException("Incorrect EVM path")
    }
    checkText(basePackageField, "Base package")
    checkText(genOutputPath, "Stubs output folder")

    settings.pathToEvm = evmPath
    settings.pathToDb = fromPath(evmDbPath)
    settings.solcPath = fromPath(standaloneSolc)
    settings.useSolcEthereum = useSolcEthereum.isSelected
    settings.useSolcJ = useSolcJ.isSelected
    settings.generateJavaStubs = generateJavaStubs.isSelected
    settings.basePackage = basePackageField.text
    settings.genStyle = generationStyle()
    settings.genOutputPath = genOutputPath.text
    ApplicationManager.getApplication().messageBus.syncPublisher(SoliditySettingsListener.TOPIC).settingsChanged()

    if (useSolcJ.isSelected) {
      val version = Solc.getVersion()
      if (version.isNotBlank()) {
        solcVersion.text = version
      } else {
        throw ConfigurationException("No solc installation found")
      }
    }
  }

  private fun checkText(field: JTextField, fieldName: String) {
    if (field.text.isBlank()) {
      throw ConfigurationException("$fieldName can't be empty")
    }
  }

  private fun generationStyle(): Sol2JavaGenerationStyle = when {
    web3jBtn.isSelected -> Sol2JavaGenerationStyle.WEB3J
    ethJNativeBtn.isSelected -> Sol2JavaGenerationStyle.ETHJ
    else -> throw IllegalStateException("Incorrect gen style option")
  }

  private fun fromPath(textField: TextFieldWithBrowseButton) = FileUtil.toSystemIndependentName(textField.text.trim())

  internal fun isModified(settings: SoliditySettings): Boolean =
    fromPath(evmPath) != settings.pathToEvm.trim() ||
      fromPath(evmDbPath) != settings.pathToDb.trim() ||
      fromPath(standaloneSolc) != settings.solcPath.trim() ||
      useSolcEthereum.isSelected != settings.useSolcEthereum ||
      useSolcJ.isSelected != settings.useSolcJ ||
      generateJavaStubs.isSelected != settings.generateJavaStubs ||
      basePackageField.text != settings.basePackage ||
      generationStyle() != settings.genStyle ||
      genOutputPath.text != settings.genOutputPath.trim()
}
