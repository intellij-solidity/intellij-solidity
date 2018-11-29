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
      override fun createTemplateConfiguration(p: Project): RunConfiguration {
        val configurationModule = SolidityRunConfigModule(p)
        return if (hasJavaSupport) SolidityRunConfig(configurationModule, this)
        else UnsupportedSolidityRunConfig(configurationModule, this)
      }

      override fun getIcon(): Icon {
        return SolidityIcons.FUNCTION
      }

      override fun isApplicable(project: Project): Boolean {
        return hasJavaSupport
      }
    }
  }

  companion object {
    const val noJavaWarning = "Current IDE platform does not support execution of Solidity contracts"
    fun getInstance(): SolidityConfigurationType {
      return ConfigurationTypeUtil.findConfigurationType<SolidityConfigurationType>(SolidityConfigurationType::class.java)
    }
  }
}

val hasJavaSupport = try {
  Class.forName("com.intellij.execution.CommonJavaRunConfigurationParameters")
  true
} catch (e: ClassNotFoundException) {
  false
}
