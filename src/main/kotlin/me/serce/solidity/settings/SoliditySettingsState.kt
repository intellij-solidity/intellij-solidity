package me.serce.solidity.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class ConfigurationMode {
  AUTOMATIC,
  MANUAL
}

@ApiStatus.Internal
enum class FormatterType {
  INTELLIJ_SOLIDITY,
  FOUNDRY,
  PRETTIER
}

@Service
@ApiStatus.Internal
class SoliditySettingsState : BaseState() {
  var formatterType by enum(FormatterType.INTELLIJ_SOLIDITY)
  var configurationMode by enum(ConfigurationMode.AUTOMATIC)
  var executablePath by string()
  var configPath by string()

}
