package me.serce.solidity.settings

import com.intellij.lang.javascript.JavaScriptBundle
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.dsl.builder.MutableProperty
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import javax.swing.JRadioButton

private const val HELP_TOPIC = "reference.settings.solidity"

class SolidityConfigurable(internal val project: Project) :
  BoundSearchableConfigurable(
    "Solidity",
    HELP_TOPIC, CONFIGURABLE_ID
  ) {

  lateinit var disabledConfiguration: JRadioButton
  private lateinit var automaticConfiguration: JRadioButton
  private lateinit var manualConfiguration: JRadioButton

  override fun createPanel(): DialogPanel {
    val settings: SoliditySettings = SoliditySettings.getInstance(project)

    // *********************
    // Configuration mode row
    // *********************
    return panel {
      buttonsGroup {
        row {
          disabledConfiguration =
            radioButton(
              JavaScriptBundle.message(
                "settings.javascript.linters.autodetect.disabled",
                displayName
              )
            ).bindSelected(
              ConfigurationModeProperty(
                settings,
                ConfigurationMode.DISABLED
              )
            ).component
        }
        row {
          automaticConfiguration =
            radioButton(
              JavaScriptBundle.message(
                "settings.javascript.linters.autodetect.configure.automatically",
                displayName
              )
            ).bindSelected(
              ConfigurationModeProperty(
                settings,
                ConfigurationMode.AUTOMATIC
              )
            ).component

          val detectAutomaticallyHelpText =
            JavaScriptBundle.message(
              "settings.javascript.linters.autodetect.configure.automatically.help.text",
              ApplicationNamesInfo.getInstance().fullProductName,
              displayName,
              "solidity.json"
            )

          val helpLabel = ContextHelpLabel.create(detectAutomaticallyHelpText)
          helpLabel.border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)
          cell(helpLabel)
        }
        row {
          manualConfiguration =
            radioButton(
              JavaScriptBundle.message(
                "settings.javascript.linters.autodetect.configure.manually",
                displayName
              )
            ).bindSelected(
              ConfigurationModeProperty(
                settings,
                ConfigurationMode.MANUAL
              )
            ).component
        }
      }

    }
  }

  private class ConfigurationModeProperty(
    private val settings: SoliditySettings,
    private val mode: ConfigurationMode,
  ) : MutableProperty<Boolean> {
    override fun get(): Boolean = settings.configurationMode == mode

    override fun set(value: Boolean) {
      if (value) {
        settings.configurationMode = mode
      }
    }
  }

  companion object {
    const val CONFIGURABLE_ID = "Settings.Solidity"
  }
}
