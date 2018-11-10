package me.serce.solidity.ide.run

import com.intellij.execution.configurations.*
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
        return if (hasJavaSupport)
          SolidityRunConfig(SolidityRunConfigModule(p0), this)
        else object : UnknownRunConfiguration(this, p0) {
          override fun checkConfiguration() {
            throw RuntimeConfigurationException(noJavaWarning)
          }
        }
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
