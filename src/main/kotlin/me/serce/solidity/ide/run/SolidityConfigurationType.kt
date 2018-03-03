package me.serce.solidity.ide.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import me.serce.solidity.ide.SolidityIcons
import javax.swing.Icon

class SolidityConfigurationType : ConfigurationTypeBase("SolidityConfigurationType", "Solidity", "Run Solidity Contract", SolidityIcons.FILE_ICON) {
  init {
    addFactory(configurationFactory())
  }

  private fun configurationFactory(): ConfigurationFactory {
    return object : ConfigurationFactory(this) {
      override fun createTemplateConfiguration(p0: Project): RunConfiguration {
        return SolidityRunConfig(SolidityRunConfigModule(p0), this)
      }

      override fun getIcon(): Icon {
        return SolidityIcons.FUNCTION
      }
    }
  }

  companion object {
    fun getInstance(): SolidityConfigurationType {
      return ConfigurationTypeUtil.findConfigurationType<SolidityConfigurationType>(SolidityConfigurationType::class.java)
    }
  }
}
