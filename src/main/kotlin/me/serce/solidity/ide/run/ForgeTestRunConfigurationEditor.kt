package me.serce.solidity.ide.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.RawCommandLineEditor
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class ForgeTestRunConfigurationEditor : SettingsEditor<ForgeTestRunConfiguration>() {
  private lateinit var mainPanel: JPanel
  private lateinit var contractNameField: RawCommandLineEditor
  private lateinit var testNameField: RawCommandLineEditor
  private lateinit var workingDirectoryField: TextFieldWithBrowseButton

  init {
    createUI()
  }

  private fun createUI() {
    contractNameField = RawCommandLineEditor()
    testNameField = RawCommandLineEditor()
    workingDirectoryField = TextFieldWithBrowseButton().apply {
      addBrowseFolderListener(
        "Select Working Directory",
        "Select the working directory for Forge",
        null,
        FileChooserDescriptorFactory.createSingleFolderDescriptor()
      )
    }

    mainPanel = FormBuilder.createFormBuilder()
      .addLabeledComponent("Test contract (leave empty for all contracts):", contractNameField)
      .addLabeledComponent("Test name (leave empty for all tests):", testNameField)
      .addLabeledComponent("Working directory:", workingDirectoryField)
      .addComponentFillVertically(JPanel(), 0)
      .panel
  }

  override fun resetEditorFrom(configuration: ForgeTestRunConfiguration) {
    contractNameField.text = configuration.contractName
    testNameField.text = configuration.testName
    workingDirectoryField.text = configuration.workingDirectory
  }

  override fun applyEditorTo(configuration: ForgeTestRunConfiguration) {
    configuration.contractName = contractNameField.text
    configuration.testName = testNameField.text
    configuration.workingDirectory = workingDirectoryField.text
  }

  override fun createEditor(): JComponent = mainPanel
}
