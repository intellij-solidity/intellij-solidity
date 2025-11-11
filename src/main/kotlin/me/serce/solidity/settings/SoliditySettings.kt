package me.serce.solidity.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
@State(name = "SoliditySettings", storages = [(Storage("solidity.xml"))])
class SoliditySettings :
  SimplePersistentStateComponent<SoliditySettingsState>(SoliditySettingsState()) {

  var formatterConfigurationMode: ConfigurationMode
    get() = state.formatterConfigurationMode
    set(value) {
      state.formatterConfigurationMode = value
    }

  var formatterType: FormatterType
    get() = state.formatterType
    set(value) {
      state.formatterType = value
    }

  var formatterFoundryExecutablePath: String
    get() = state.formatterFoundryExecutablePath ?: ""
    set(value) {
      state.formatterFoundryExecutablePath = value
    }

  var formatterFoundryConfigPath: String
    get() = state.formatterFoundryConfigPath ?: ""
    set(value) {
      val file = File(value)
      if (file.isFile) {
        state.formatterFoundryConfigPath = file.parentFile.path
        return
      }
      state.formatterFoundryConfigPath = value
    }

  var testFoundryConfigurationMode: ConfigurationMode
    get() = state.testFoundryConfigurationMode
    set(value) {
      state.testFoundryConfigurationMode = value
    }

  var testFoundryExecutablePath: String
    get() = state.testFoundryExecutablePath ?: ""
    set(value) {
      state.testFoundryExecutablePath = value
    }

  var testFoundryConfigPath: String
    get() = state.testFoundryConfigPath ?: ""
    set(value) {
      val file = File(value)
      if (file.isFile) {
        state.testFoundryConfigPath = file.parentFile.path
        return
      }
      state.testFoundryConfigPath = value
    }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SoliditySettings = project.service()
  }
}
