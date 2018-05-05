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
    if (hasJavaSupport) {
      addFactory(configurationFactory())
    }
  }

  private fun configurationFactory(): ConfigurationFactory {
    return object : ConfigurationFactory(this) {
      override fun createTemplateConfiguration(p0: Project): RunConfiguration {
        return SolidityRunConfig(SolidityRunConfigModule(p0), this)
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
