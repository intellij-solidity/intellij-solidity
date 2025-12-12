package me.serce.solidity.ide.formatting

import com.intellij.application.options.*
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.*
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.loadCodeSampleResource
import org.jdom.Element
import javax.swing.JCheckBox

class SolCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
  override fun getLanguage(): Language = SolidityLanguage

  override fun createCustomSettings(settings: CodeStyleSettings) = SolCodeStyleSettings(true, settings)

  override fun getConfigurableDisplayName() = SolidityLanguage.displayName

  override fun createConfigurable(settings: CodeStyleSettings, originalSettings: CodeStyleSettings) =
    object : CodeStyleAbstractConfigurable(settings, originalSettings, configurableDisplayName) {
      override fun createPanel(settings: CodeStyleSettings) = SolCodeStyleMainPanel(currentSettings, settings)
      override fun getHelpTopic() = null
    }

  private class SolCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(SolidityLanguage, currentSettings, settings) {

    override fun initTabs(settings: CodeStyleSettings) {
      addIndentOptionsTab(settings)
      addTab(ImportSettingsPanelWrapper(settings))
    }
  }
}

class SolCodeStyleSettings(@JvmField var specificSymbolImports: Boolean, container: CodeStyleSettings) : CustomCodeStyleSettings("SolCodeStyleSettings", container) {

  override fun readExternal(parentElement: Element?) {
    super.readExternal(parentElement)
  }

  override fun writeExternal(parentElement: Element?, parentSettings: CustomCodeStyleSettings) {
    super.writeExternal(parentElement, parentSettings)
  }
}

val CodeStyleSettings.solidityCustomSettings: SolCodeStyleSettings
    get() = getCustomSettings(SolCodeStyleSettings::class.java)

class SolLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun getLanguage(): Language = SolidityLanguage

  override fun getCodeSample(settingsType: SettingsType): String =
    when (settingsType) {
      SettingsType.INDENT_SETTINGS -> INDENT_SAMPLE
      else -> ""
    }

  override fun customizeDefaults(
    commonSettings: CommonCodeStyleSettings,
    indentOptions: CommonCodeStyleSettings.IndentOptions
  ) {
  }

  override fun getIndentOptionsEditor(): IndentOptionsEditor? = SmartIndentOptionsEditor()

  private val INDENT_SAMPLE: String by lazy {
    loadCodeSampleResource(this, "me/serce/solidity/ide/formatting/indent_sample.sol")
  }
}


class ImportSettingsPanelWrapper(settings: CodeStyleSettings) : CodeStyleAbstractPanel(SolidityLanguage, null, settings) {

  private val importsPanel = ImportSettingsPanel()
  private val content = JBScrollPane(importsPanel.panel)

  override fun getRightMargin() = throw UnsupportedOperationException()

  override fun createHighlighter(scheme: EditorColorsScheme) = throw UnsupportedOperationException()

  override fun getFileType() = throw UnsupportedOperationException()

  override fun getPreviewText(): String? = null

  override fun apply(settings: CodeStyleSettings) = importsPanel.apply(settings.solCodeStyleSettings())

  private fun CodeStyleSettings.solCodeStyleSettings() =
    getCustomSettings(SolCodeStyleSettings::class.java)

  override fun isModified(settings: CodeStyleSettings) = importsPanel.isModified(settings.solCodeStyleSettings())

  override fun getPanel() = content

  override fun resetImpl(settings: CodeStyleSettings) {
    importsPanel.reset(settings.solCodeStyleSettings())
  }

  override fun getTabTitle() = ApplicationBundle.message("title.imports")
}

private class ImportSettingsPanel {
  private lateinit var addSpecificImports: JCheckBox


  val panel = panel {
      row {
        addSpecificImports = checkBox("Add specific symbol names on auto-importing")
          .component
      }


  }.apply {
    border = JBUI.Borders.empty(0, 10, 10, 10)
  }

  fun reset(settings: SolCodeStyleSettings) {
    addSpecificImports.isSelected = settings.specificSymbolImports

  }

  fun apply(settings: SolCodeStyleSettings) {
    settings.specificSymbolImports = addSpecificImports.isSelected
  }

  fun isModified(settings: SolCodeStyleSettings): Boolean {
    return with(settings) {
      var isModified = false
      isModified = isModified || isModified(addSpecificImports, specificSymbolImports)
      isModified
    }
  }

  companion object {
    private fun isModified(checkBox: JCheckBox, value: Boolean): Boolean {
      return checkBox.isSelected != value
    }


  }

}
