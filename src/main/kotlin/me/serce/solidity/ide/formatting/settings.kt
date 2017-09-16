package me.serce.solidity.ide.formatting

import com.intellij.application.options.CodeStyleAbstractConfigurable
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.application.options.TabbedLanguageCodeStylePanel
import com.intellij.lang.Language
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider
import com.intellij.psi.codeStyle.CustomCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import me.serce.solidity.lang.SolidityLanguage

class SolCodeStyleSettingsProvider : CodeStyleSettingsProvider() {
  override fun createCustomSettings(settings: CodeStyleSettings) = SolCodeStyleSettings(settings)

  override fun getConfigurableDisplayName() = SolidityLanguage.displayName

  override fun createSettingsPage(settings: CodeStyleSettings, originalSettings: CodeStyleSettings) =
    object : CodeStyleAbstractConfigurable(settings, originalSettings, configurableDisplayName) {
      override fun createPanel(settings: CodeStyleSettings) = SolCodeStyleMainPanel(currentSettings, settings)
      override fun getHelpTopic() = null
    }

  private class SolCodeStyleMainPanel(currentSettings: CodeStyleSettings, settings: CodeStyleSettings) :
    TabbedLanguageCodeStylePanel(SolidityLanguage, currentSettings, settings) {

    override fun initTabs(settings: CodeStyleSettings?) {
      addIndentOptionsTab(settings)
    }
  }
}

class SolCodeStyleSettings(container: CodeStyleSettings) : CustomCodeStyleSettings("SolCodeStyleSettings", container)


class SolLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
  override fun getLanguage(): Language = SolidityLanguage

  override fun getCodeSample(settingsType: SettingsType): String =
    when (settingsType) {
      SettingsType.INDENT_SETTINGS -> INDENT_SAMPLE
      else -> ""
    }

  override fun getIndentOptionsEditor(): IndentOptionsEditor? = SmartIndentOptionsEditor()

  private val INDENT_SAMPLE: String by lazy {
    loadCodeSampleResource("me/serce/solidity/ide/formatting/indent_sample.sol")
  }

  private fun loadCodeSampleResource(resource: String): String {
    val stream = this.javaClass.classLoader.getResourceAsStream(resource)
    // We need to convert line separators here, because IntelliJ always expects \n,
    // while on Windows the resource file will be read with \r\n as line separator.
    return StreamUtil.convertSeparators(StreamUtil.readText(stream, "UTF-8"))
  }
}
