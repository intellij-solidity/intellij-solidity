package me.serce.solidity.settings

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.and
import com.intellij.ui.layout.selected
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JRadioButton
import kotlin.io.path.Path

private const val HELP_TOPIC = "reference.settings.solidity"

class SolidityConfigurable(internal val project: Project) : BoundSearchableConfigurable(
  "Solidity", HELP_TOPIC, CONFIGURABLE_ID
) {

  lateinit var intellijSolidityFormatter: JRadioButton
  private lateinit var foundryFormatter: JRadioButton
  private lateinit var disabledFormatter: JRadioButton
  private lateinit var foundryFormatterAutomaticConfiguration: JRadioButton
  private lateinit var foundryFormatterManualConfiguration: JRadioButton

  private lateinit var foundryTestAutomaticConfiguration: JRadioButton
  private lateinit var foundryTestManualConfiguration: JRadioButton

  override fun createPanel(): DialogPanel {
    val settings: SoliditySettings = SoliditySettings.getInstance(project)

    return panel {
      collapsibleGroup("Formatter") {
        buttonsGroup {
          row {
            intellijSolidityFormatter = radioButton(
              "Intellij-Solidity"
            ).bindSelected(
              FormatterTypeProperty(
                settings, FormatterType.INTELLIJ_SOLIDITY
              )
            ).component
          }
          row {
            foundryFormatter = radioButton(
              "Foundry"
            ).bindSelected(
              FormatterTypeProperty(
                settings, FormatterType.FOUNDRY
              )
            ).component
          }
          row {
            disabledFormatter = radioButton(
              "Disabled"
            ).bindSelected(
              FormatterTypeProperty(
                settings, FormatterType.DISABLED
              )
            ).component
            val helpLabel =
              ContextHelpLabel.create("Disable the built-in formatter to enable external formatters such as Prettier")
            helpLabel.border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)
            cell(helpLabel)
          }
        }
        buttonsGroup {
          row {
            foundryFormatterAutomaticConfiguration = radioButton(
              "Automatic Foundry configuration"
            ).bindSelected(
              FormatterConfigurationModeProperty(
                settings, ConfigurationMode.AUTOMATIC
              )
            ).component

            val detectAutomaticallyHelpText =
              ApplicationNamesInfo.getInstance().fullProductName + " will use the forge executable installed at " + "USER_HOME/.foundry/bin/forge and the foundry.toml configuration file located in the same " + "folder as the current file or any of its parent folders."

            val helpLabel = ContextHelpLabel.create(detectAutomaticallyHelpText)
            helpLabel.border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)
            cell(helpLabel)
          }
          row {
            foundryFormatterManualConfiguration = radioButton(
              "Manual Foundry configuration"
            ).bindSelected(
              FormatterConfigurationModeProperty(
                settings, ConfigurationMode.MANUAL
              )
            ).component
          }
        }.visibleIf(foundryFormatter.selected)

        panel {
          row("Forge executable") {
            textFieldWithBrowseButton("Forge executable") { fileChosen(it) }.bindText(
              settings::formatterFoundryExecutablePath
            )
          }

          row("Path of foundry.toml") {
            textFieldWithBrowseButton(
              "Path of foundry.toml",
              project,
            ) { fileChosen(it) }.bindText(settings::formatterFoundryConfigPath).validationOnInput(validateFoundryTomlConfigDir())
          }
        }.visibleIf(foundryFormatterManualConfiguration.selected.and(foundryFormatter.selected))
      }.expanded = true

      collapsibleGroup("Foundry Test Configuration") {
        buttonsGroup {
          row {
            foundryTestAutomaticConfiguration = radioButton(
              "Automatic Foundry configuration"
            ).bindSelected(
              TestConfigurationModeProperty(
                settings, ConfigurationMode.AUTOMATIC
              )
            ).component

            val detectAutomaticallyHelpText =
              ApplicationNamesInfo.getInstance().fullProductName + " will use the forge executable installed at " + "USER_HOME/.foundry/bin/forge and the foundry.toml configuration file located in the same " + "folder as the current file or any of its parent folders."

            val helpLabel = ContextHelpLabel.create(detectAutomaticallyHelpText)
            helpLabel.border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)
            cell(helpLabel)
          }
          row {
            foundryTestManualConfiguration = radioButton(
              "Manual Foundry configuration"
            ).bindSelected(
              TestConfigurationModeProperty(
                settings, ConfigurationMode.MANUAL
              )
            ).component
          }
        }

        panel {
          row("Forge executable") {
            textFieldWithBrowseButton("Forge executable") { fileChosen(it) }.bindText(
              settings::testFoundryExecutablePath
            )
          }

          row("Path of foundry.toml") {
            textFieldWithBrowseButton(
              "Path of foundry.toml",
              project,
            ) { fileChosen(it) }.bindText(settings::testFoundryConfigPath).validationOnInput(validateFoundryTomlConfigDir())
          }
        }.visibleIf(foundryTestManualConfiguration.selected)
      }.expanded = true
    }
  }

  private class FormatterTypeProperty(
    private val settings: SoliditySettings, private val formatterType: FormatterType
  ) : MutableProperty<Boolean> {
    override fun get(): Boolean = settings.formatterType == formatterType

    override fun set(value: Boolean) {
      if (value) {
        settings.formatterType = formatterType
      }
    }
  }

  private class FormatterConfigurationModeProperty(
    private val settings: SoliditySettings,
    private val mode: ConfigurationMode,
  ) : MutableProperty<Boolean> {
    override fun get(): Boolean = settings.formatterConfigurationMode == mode

    override fun set(value: Boolean) {
      if (value) {
        settings.formatterConfigurationMode = mode
      }
    }
  }

  private class TestConfigurationModeProperty(
    private val settings: SoliditySettings,
    private val mode: ConfigurationMode,
  ) : MutableProperty<Boolean> {
    override fun get(): Boolean = settings.testFoundryConfigurationMode == mode

    override fun set(value: Boolean) {
      if (value) {
        settings.testFoundryConfigurationMode = mode
      }
    }
  }

  private fun validateFoundryTomlConfigDir(): ValidationInfoBuilder.(TextFieldWithBrowseButton) -> ValidationInfo? = {
    val selected = VfsUtil.findFile(Path(it.text), true)
    if (selected == null || !selected.exists()) {
      ValidationInfo("Failed to locate foundry.toml configuration file", it)
    } else {
      if (!selected.isFoundryToml()) {
        ValidationInfo("Failed to locate foundry.toml configuration file", it)
      } else {
        null
      }
    }
  }

  private fun VirtualFile.isFoundryToml() = name == "foundry.toml"


  private fun fileChosen(file: VirtualFile): String {
    return file.path
  }

  companion object {
    const val CONFIGURABLE_ID = "Settings.Solidity"
  }
}
