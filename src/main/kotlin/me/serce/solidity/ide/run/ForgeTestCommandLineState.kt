package me.serce.solidity.ide.run

import com.intellij.execution.*
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties

class ForgeTestCommandLineState(
  private val configuration: ForgeTestRunConfiguration,
  environment: ExecutionEnvironment
) : CommandLineState(environment) {
  companion object {
    private val USER_HOME = System.getProperty("user.home")
  }

  @Throws(ExecutionException::class)
  override fun startProcess(): ProcessHandler {
    val cmd = GeneralCommandLine()
      .withWorkDirectory(configuration.workingDirectory)
      .withExePath("$USER_HOME/.foundry/bin/forge")
      .withParameters("test")
      .withParameters("-vvvv")
    configuration.testName.takeIf { it.isNotBlank() }?.let { testName ->
      cmd.addParameter("--match-test")
      cmd.addParameter(testName)
    }
    configuration.contractName.takeIf { it.isNotBlank() }?.let { contractName ->
      cmd.addParameter("--match-contract")
      cmd.addParameter(contractName)
    }
    return ForgeTestTransformingProcessHandler(cmd)
  }

  @Throws(ExecutionException::class)
  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val properties = SMTRunnerConsoleProperties(configuration, "xUnit", executor)
    val console = SMTestRunnerConnectionUtil.createConsole("Foundry", properties)
    val processHandler = startProcess()
    console.attachToProcess(processHandler)
    return DefaultExecutionResult(console, processHandler, *createActions(console, processHandler))
  }
}
