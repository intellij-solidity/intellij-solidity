package me.serce.solidity.settings

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class ConfigurationMode {
  DISABLED,
  AUTOMATIC,
  MANUAL
}

@Service
@ApiStatus.Internal
class SoliditySettingsState : BaseState() {
  var configurationMode by enum(ConfigurationMode.AUTOMATIC)

}
