package me.serce.solidity.ide.run

import com.intellij.diagnostic.logging.LogConfigurationPanel
import com.intellij.execution.*
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorGroup
import com.intellij.openapi.options.ShowSettingsUtil
import me.serce.solidity.ide.run.ui.SolidityConfigurableEditorPanel
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsConfigurable
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import org.jdom.Element
import java.util.*


class SolidityRunConfig(configurationModule: SolidityRunConfigModule?, factory: ConfigurationFactory?) : ModuleBasedConfiguration<SolidityRunConfigModule>(configurationModule, factory), CommonJavaRunConfigurationParameters {

  var myData: Data = Data()

  override fun getEnvs(): Map<String, String> {
    return myData.envs;
  }

  override fun setAlternativeJrePath(p0: String?) {
    myData.ajre = p0
  }

  override fun isPassParentEnvs(): Boolean {
    return true
  }

  override fun setProgramParameters(p0: String?) {
    myData.programParameters = p0
  }

  override fun setVMParameters(p0: String) {
    myData.vmParameters = p0
  }

  override fun isAlternativeJrePathEnabled(): Boolean {
    return myData.ajreEnabled
  }

  override fun getPackage(): String? {
    throw UnsupportedOperationException("Should not be called for this class")
  }

  override fun getRunClass(): String? {
    throw UnsupportedOperationException("Should not be called for this class")
  }

  override fun getWorkingDirectory(): String? {
    return myData.getWorkingDirectory()
  }

  override fun setAlternativeJrePathEnabled(p0: Boolean) {
    myData.ajreEnabled = p0
  }

  override fun getVMParameters(): String? {
    return myData.vmParameters
  }

  override fun setWorkingDirectory(p0: String?) {
    myData.setWorkingDirectory(p0)
  }

  override fun setEnvs(p0: MutableMap<String, String>) {
    myData.envs.clear()
    myData.envs.putAll(p0)
  }

  override fun setPassParentEnvs(p0: Boolean) {
  }

  override fun getProgramParameters(): String? {
    return myData.programParameters;
  }

  override fun getAlternativeJrePath(): String? {
    return myData.ajre
  }

  override fun getValidModules(): Collection<Module> {
    return ModuleManager.getInstance(project).modules.asList()
  }

  override fun getState(p0: Executor, env: ExecutionEnvironment): RunProfileState? {
    val state = SolidityRunState( env, this)
    state.consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
    return state
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    val group = SettingsEditorGroup<SolidityRunConfig>()
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title", *arrayOfNulls(0)), SolidityConfigurableEditorPanel(this.project))
    group.addEditor(ExecutionBundle.message("logs.tab.title", *arrayOfNulls(0)), LogConfigurationPanel())
    return group
  }


  @Suppress("DEPRECATION")
  override fun readExternal(element: Element?) {
    super.readExternal(element)
    JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element!!)
    com.intellij.openapi.util.DefaultJDOMExternalizer.readExternal(this, element)
    com.intellij.openapi.util.DefaultJDOMExternalizer.readExternal(myData, element)

    readModule(element)
    EnvironmentVariablesComponent.readExternal(element, envs)
  }

  @Suppress("DEPRECATION")
  override fun writeExternal(element: Element?) {
    super.writeExternal(element)
    JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element!!)
    com.intellij.openapi.util.DefaultJDOMExternalizer.writeExternal(this, element)
    com.intellij.openapi.util.DefaultJDOMExternalizer.writeExternal(myData, element)
    writeModule(element)

    EnvironmentVariablesComponent.writeExternal(element, envs)
  }


  fun getPersistentData(): Data {
    return myData
  }

  data class Data(val u: Unit? = null) : Cloneable {
    @JvmField
    var contractName: String? = null
    @JvmField
    var functionName: String? = null
    @JvmField
    var vmParameters: String? = null
    @JvmField
    var programParameters: String? = null
    @JvmField
    var workingDirectory: String? = null
    @JvmField
    var envs: MutableMap<String, String> = LinkedHashMap()
    @JvmField
    var ajreEnabled = false;
    @JvmField
    var ajre: String? = null;

    public override fun clone(): Data {
      try {
        val data = super.clone() as Data
        data.envs = LinkedHashMap(envs)
        return data
      } catch (e: CloneNotSupportedException) {
        throw RuntimeException(e)
      }
    }

    fun getWorkingDirectory(): String = ExternalizablePath.localPathValue(workingDirectory)

    fun setWorkingDirectory(value: String?) {
      workingDirectory = ExternalizablePath.urlValue(value)
    }

    fun getGetContractName(): String = contractName ?: ""

    fun getGetFunctionName(): String = functionName ?: ""

    fun setContract(contract: SolContractDefinition?) {
      contractName = contract?.name
    }

    fun setFunction(methodLocation: SolFunctionDefinition) {
      setContract(methodLocation.contract)
      functionName = methodLocation.name!!
    }
  }

  override fun checkConfiguration() {
    if (!SoliditySettings.instance.validateEvm()) {
      throw RuntimeConfigurationError("Configure EVM", {ShowSettingsUtil.getInstance().editConfigurable(project, SoliditySettingsConfigurable(SoliditySettings.instance))})
    }
    JavaParametersUtil.checkAlternativeJRE(this)
    val configurationModule = configurationModule
    val psiContract = SearchUtils.findContract(myData.getGetContractName(), configurationModule.project)
    if (psiContract != null && myData.functionName != null && psiContract.functionDefinitionList.none { it.name == myData.functionName }) {
      throw RuntimeConfigurationWarning("Can't find method '${myData.functionName}' within contract '${psiContract.name}'" )
    }
    ProgramParametersUtil.checkWorkingDirectoryExist(this, project, configurationModule.module)
    JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)
  }
}
