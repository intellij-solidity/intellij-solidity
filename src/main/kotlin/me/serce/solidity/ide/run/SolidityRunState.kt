package me.serce.solidity.ide.run

import com.intellij.execution.application.BaseJavaApplicationCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.util.PathUtil
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.run.EthereumRunner
import java.io.File


class SolidityRunState(environment: ExecutionEnvironment?, configuration: SolidityRunConfig) : BaseJavaApplicationCommandLineState<SolidityRunConfig>(environment, configuration) {
  override fun createJavaParameters(): JavaParameters {
    val params = JavaParameters()
    val jreHome = if (myConfiguration.isAlternativeJrePathEnabled) myConfiguration.alternativeJrePath else null
    params.jdk = JavaParametersUtil.createProjectJdk(myConfiguration.project, jreHome)
    setupJavaParameters(params)
    params.mainClass = EthereumRunner::class.qualifiedName
    val sourceRoots = ModuleRootManager.getInstance(configuration.configurationModule.module!!).sourceRoots
    val srcPaths = sourceRoots.map { it.path }
    val mainContract = configuration.getPersistentData().contractName!!
    params.programParametersList.add(mainContract)
    params.programParametersList.add(configuration.getPersistentData().functionName)
    params.programParametersList.addAll(srcPaths)

    params.configureByModule(configuration.configurationModule.module, JavaParameters.JDK_AND_CLASSES)
    params.classPath.add(PathUtil.getJarPathForClass(EthereumRunner::class.java))
    params.classPath.add(SoliditySettings.instance.pathToEvm + File.separator + "*")
    if (!SoliditySettings.instance.pathToDb.isNullOrBlank()) {
      params.vmParametersList.add("-Devm.database.dir=${SoliditySettings.instance.pathToDb}")
    }
    return params
  }


}
