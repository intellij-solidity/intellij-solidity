package me.serce.solidity.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "SoliditySettings", storages = [(Storage("solidity.xml"))])
class SoliditySettings :
  SimplePersistentStateComponent<SoliditySettingsState>(SoliditySettingsState()) {

  var configurationMode: ConfigurationMode
    get() = state.configurationMode
    set(value) {
      state.configurationMode = value
    }

  fun isEnabled(): Boolean {
    return configurationMode !== ConfigurationMode.DISABLED
  }


  companion object {
    @JvmStatic
    fun getInstance(project: Project): SoliditySettings = project.service()
  }
}
