package me.serce.solidity.ide.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import me.serce.solidity.ide.SolidityIcons

class ForgeTestRunConfigurationType : ConfigurationType {
  override fun getId() = "ForgeTestRunConfigurationType"
  override fun getIcon() = SolidityIcons.FILE_ICON
  override fun getDisplayName() = "Forge Test"
  override fun getConfigurationTypeDescription() = "Forge test configuration"
  override fun getConfigurationFactories() = arrayOf(ForgeTestRunConfigurationFactory(this))
}

class ForgeTestRunConfigurationFactory(type: ForgeTestRunConfigurationType) : ConfigurationFactory(type) {
  override fun getId() = "ForgeTestRunConfigurationFactory"

  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return ForgeTestRunConfiguration(project, this, "Forge Test")
  }

  override fun isApplicable(project: Project): Boolean =
    Registry.`is`("solidity.forge.enabled", true)
}
