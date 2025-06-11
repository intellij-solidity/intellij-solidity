package me.serce.solidity.ide.run

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.Visibility

class ForgeTestRunConfigurationProducer : LazyRunConfigurationProducer<ForgeTestRunConfiguration>() {
  override fun getConfigurationFactory() = ForgeTestRunConfigurationFactory(ForgeTestRunConfigurationType())

  override fun isConfigurationFromContext(
    configuration: ForgeTestRunConfiguration,
    context: ConfigurationContext
  ): Boolean {
    val location = context.location ?: return false
    val element = location.psiElement

    return when (element) {
      is SolFunctionDefinition -> {
        element.isTestFunction() &&
          configuration.testName == element.name &&
          configuration.contractName == element.contract?.name
      }

      is SolContractDefinition -> {
        configuration.contractName == element.name
      }

      else -> false
    }
  }

  override fun setupConfigurationFromContext(
    configuration: ForgeTestRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>
  ): Boolean {
    val location = context.location ?: return false
    val element = location.psiElement
    val project = context.project

    when (element) {
      is SolFunctionDefinition -> {
        if (element.isTestFunction()) {
          configuration.name = "${element.contract?.name}.${element.name}"
          configuration.testName = element.name ?: ""
          configuration.contractName = element.contract?.name ?: ""
          configuration.workingDirectory = project.basePath ?: ""
          sourceElement.set(element)
          return true
        }
      }

      is SolContractDefinition -> {
        configuration.name = element.name ?: ""
        configuration.contractName = element.name ?: ""
        configuration.workingDirectory = project.basePath ?: ""
        sourceElement.set(element)
        return true
      }
    }

    return false
  }

  fun runTest(project: Project, fullTestName: String) {
    val configuration = ForgeTestRunConfiguration(project, configurationFactory, "Forge Test - $fullTestName")
    configuration.workingDirectory = project.basePath ?: ""

    val regex = Regex("(\\w+)\\.?(\\w+)?")
    regex.matchEntire(fullTestName)?.let { m ->
      configuration.contractName = m.destructured.component1()
      configuration.testName = m.destructured.component2()
    }

    val runManager = RunManager.getInstance(project)
    val runnerAndConfigurationSettings = runManager.createConfiguration(configuration, configurationFactory)
    ProgramRunnerUtil.executeConfiguration(
      runnerAndConfigurationSettings,
      DefaultRunExecutor.getRunExecutorInstance()
    )
  }
}

private fun SolFunctionDefinition.isTestFunction(): Boolean {
  // Check if function name starts with 'test' and is public/external
  return name?.startsWith("test") == true &&
    (visibility == null || visibility == Visibility.PUBLIC || visibility == Visibility.EXTERNAL)
}
