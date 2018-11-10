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
import me.serce.solidity.ide.run.SolidityConfigurationType
import me.serce.solidity.ide.run.compile.Solc
import me.serce.solidity.ide.run.hasJavaSupport
import java.awt.Component
import java.io.File
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
  private lateinit var solcAddtionalOptions: JTextField

  private lateinit var solcVersion: JLabel

  private lateinit var useSolcJ: JCheckBox
  private lateinit var javaInteropPanel: JPanel
  private lateinit var generateJavaStubs: JCheckBox
  private lateinit var dependencyAutoRefresh: JCheckBox
  private lateinit var web3jBtn: JRadioButton
  private lateinit var ethJNativeBtn: JRadioButton
  private lateinit var basePackageField: JTextField
  private lateinit var genOutputPath: JTextField

  private lateinit var warningLabel: JLabel



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
      updateCompileAvailability()
    }

    downloadBtn.addActionListener {
      val dir = EvmDownloader.download(mainPanel)
      if (dir.isNotEmpty()) {
        evmPath.text = dir
      }
    }
    evmPath.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: javax.swing.event.DocumentEvent?) {
        updateSolcControlAvailability()
      }
    })

    val solcDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
    solcDescriptor.title = "Standalone Solc installation"
    solcDescriptor.description = "Select path to Solidity compiler"
    standaloneSolc.addBrowseFolderListener(solcDescriptor.title, solcDescriptor.description, null, solcDescriptor)

    standaloneSolc.textField.document.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent?) {
        updateSolcControlAvailability()
      }
    })

    useSolcEthereum.addActionListener {
      updateSolcControlAvailability()
    }

    generateJavaStubs.addActionListener {
      updateInteropControlsAvailability()
    }
    if (!hasJavaSupport) {
      warningLabel.icon = AllIcons.General.BalloonWarning
      warningLabel.text = SolidityConfigurationType.noJavaWarning
    } else {
      warningLabel.isVisible = false
    }
  }

  private fun updateCompileAvailability() {
    val enabled = useSolcJ.isSelected
    compilePanel.setAll({ it.isEnabled = enabled }, useSolcJ)
  }

  private fun updateInteropControlsAvailability() {
    val enabled = generateJavaStubs.isSelected
    javaInteropPanel.setAll({ it.isEnabled = enabled }, generateJavaStubs)
  }

  private fun updateSolcControlAvailability() {
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
    solcAddtionalOptions.text = settings.solcAdditionalOptions
    useSolcEthereum.isSelected = settings.useSolcEthereum
    useSolcJ.isSelected = settings.useSolcJ
    generateJavaStubs.isSelected = settings.generateJavaStubs
    dependencyAutoRefresh.isSelected = settings.dependenciesAutoRefresh
    basePackageField.text = settings.basePackage
    when (settings.genStyle) {
      Sol2JavaGenerationStyle.WEB3J -> web3jBtn.isSelected = true
      Sol2JavaGenerationStyle.ETHJ -> ethJNativeBtn.isSelected = true
    }
    genOutputPath.text = FileUtil.toSystemDependentName(settings.genOutputPath)
    updateCompileAvailability()
    updateInteropControlsAvailability()
    updateSolcControlAvailability()
    solcVersion.text = Solc.getVersion()
  }

  internal fun apply(settings: SoliditySettings) {
    validateSettings()

    settings.pathToEvm = fromPath(evmPath)
    settings.pathToDb = fromPath(evmDbPath)
    settings.solcPath = fromPath(standaloneSolc)
    settings.solcAdditionalOptions = solcAddtionalOptions.text
    settings.useSolcEthereum = useSolcEthereum.isSelected
    settings.useSolcJ = useSolcJ.isSelected
    settings.generateJavaStubs = generateJavaStubs.isSelected
    settings.basePackage = basePackageField.text
    settings.genStyle = generationStyle()
    settings.genOutputPath = genOutputPath.text
    settings.dependenciesAutoRefresh = dependencyAutoRefresh.isSelected

    ApplicationManager.getApplication().messageBus.syncPublisher(SoliditySettingsListener.TOPIC).settingsChanged()
  }

  private fun validateSettings() {
    val evmPath = fromPath(evmPath)
    if (evmPath.isNotBlank() && !SoliditySettings.validateEvm(evmPath)) {
      throw ConfigurationException("Incorrect EVM path")
    }

    if (useSolcJ.isSelected) {
      val executable = if (useSolcEthereum.isSelected) Solc.extractSolc(evmPath) else File(fromPath(standaloneSolc))
      val version = Solc.getVersion(executable)
      solcVersion.text = version
      if (version.isBlank()) {
        throw ConfigurationException("No solc installation found")
      }
    } else if (generateJavaStubs.isSelected) {
      throw ConfigurationException("Solc must be enabled to generate java stubs")
    }

    checkText(basePackageField, "Base package")
    checkText(genOutputPath, "Stubs output folder")
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
      solcAddtionalOptions.text != settings.solcAdditionalOptions ||
      useSolcEthereum.isSelected != settings.useSolcEthereum ||
      useSolcJ.isSelected != settings.useSolcJ ||
      generateJavaStubs.isSelected != settings.generateJavaStubs ||
      basePackageField.text != settings.basePackage ||
      generationStyle() != settings.genStyle ||
      genOutputPath.text != settings.genOutputPath.trim() ||
      dependencyAutoRefresh.isSelected != settings.dependenciesAutoRefresh
}
