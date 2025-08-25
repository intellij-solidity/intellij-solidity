package me.serce.solidity.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
@State(name = "SoliditySettings", storages = [(Storage("solidity.xml"))])
class SoliditySettings :
  SimplePersistentStateComponent<SoliditySettingsState>(SoliditySettingsState()) {

  var configurationMode: ConfigurationMode
    get() = state.configurationMode
    set(value) {
      state.configurationMode = value
    }

  var formatterType: FormatterType
    get() = state.formatterType
    set(value) {
      state.formatterType = value
    }

  var executablePath: String
    get() = state.executablePath ?: ""
    set(value) {
      state.executablePath = value
    }

  var configPath: String
    get() = state.configPath ?: ""
    set(value) {
      val file = File(value)
      if (file.isFile) {
        state.configPath = file.parentFile.path
        return
      }
      state.configPath = value
    }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SoliditySettings = project.service()
  }
}
