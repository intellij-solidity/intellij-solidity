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
  var formatterConfigurationMode by enum(ConfigurationMode.AUTOMATIC)
  var formatterFoundryExecutablePath by string()
  var formatterFoundryConfigPath by string()

  var testFoundryConfigurationMode by enum(ConfigurationMode.AUTOMATIC)
  var testFoundryExecutablePath by string()
  var testFoundryConfigPath by string()

}
