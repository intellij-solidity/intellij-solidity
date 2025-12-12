package me.serce.solidity.settings

import com.intellij.openapi.components.BaseState

enum class ConfigurationMode {
  AUTOMATIC,
  MANUAL
}

enum class FormatterType {
  INTELLIJ_SOLIDITY,
  FOUNDRY,
  DISABLED
}

class SoliditySettingsState : BaseState() {
  var formatterType by enum(FormatterType.INTELLIJ_SOLIDITY)
  var configurationMode by enum(ConfigurationMode.AUTOMATIC)
  var executablePath by string()
  var configPath by string()

  var testFoundryConfigurationMode by enum(ConfigurationMode.AUTOMATIC)
  var testFoundryExecutablePath by string()
  var testFoundryConfigPath by string()

}
