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
import com.intellij.util.xmlb.XmlSerializer
import me.serce.solidity.ide.run.ui.SolidityConfigurableEditorPanel
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsConfigurable
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import org.jdom.Element
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

abstract class SolidityRunConfigBase(configurationModule: SolidityRunConfigModule, factory: ConfigurationFactory) : ModuleBasedConfiguration<SolidityRunConfigModule>(configurationModule, factory), RunConfigurationWithSuppressedDefaultDebugAction

class UnsupportedSolidityRunConfig(configurationModule: SolidityRunConfigModule, factory: ConfigurationFactory) : SolidityRunConfigBase(configurationModule, factory) {
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return object : SettingsEditor<RunConfiguration>() {
      val myPanel = JPanel()

      init {
        myPanel.border = BorderFactory.createEmptyBorder(0, 0, 50, 0)
        myPanel.add(JLabel("This configuration cannot be edited", JLabel.CENTER))
      }

      override fun createEditor(): JComponent = myPanel
      override fun resetEditorFrom(s: RunConfiguration) {}
      override fun applyEditorTo(s: RunConfiguration) {}
    }
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    throw ExecutionException(SolidityConfigurationType.noJavaWarning)
  }

  override fun getValidModules(): Collection<Module> {
    throw ExecutionException(SolidityConfigurationType.noJavaWarning)
  }

  override fun checkConfiguration() {
    throw RuntimeConfigurationException(SolidityConfigurationType.noJavaWarning)
  }
}

/**
 * Extends [CommonJavaRunConfigurationParameters] which may not be accessible on some IDE (e.g. WebStorm)
 */
class SolidityRunConfig(configurationModule: SolidityRunConfigModule, factory: ConfigurationFactory) : SolidityRunConfigBase(configurationModule, factory), CommonJavaRunConfigurationParameters {

  private var myData: Data = Data()

  override fun getEnvs(): Map<String, String> {
    return myData.envs
  }

  override fun setAlternativeJrePath(ajre: String?) {
    myData.ajre = ajre
  }

  override fun isPassParentEnvs(): Boolean {
    return true
  }

  override fun setProgramParameters(progParams: String?) {
    myData.programParameters = progParams
  }

  override fun setVMParameters(vmParams: String?) {
    myData.vmParameters = vmParams
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

  override fun setAlternativeJrePathEnabled(ajreEnabled: Boolean) {
    myData.ajreEnabled = ajreEnabled
  }

  override fun getVMParameters(): String? {
    return myData.vmParameters
  }

  override fun setWorkingDirectory(workingDir: String?) {
    myData.setWorkingDirectory(workingDir)
  }

  override fun setEnvs(envs: Map<String, String>) {
    myData.envs.clear()
    myData.envs.putAll(envs)
  }

  override fun setPassParentEnvs(passParentEnvs: Boolean) {
  }

  override fun getProgramParameters(): String? {
    return myData.programParameters
  }

  override fun getAlternativeJrePath(): String? {
    return myData.ajre
  }

  override fun getValidModules(): Collection<Module> {
    return ModuleManager.getInstance(project).modules.asList()
  }

  override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
    val state = SolidityRunState(env, this)
    state.consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
    return state
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    val group = SettingsEditorGroup<SolidityRunConfig>()
    group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title", *arrayOfNulls(0)), SolidityConfigurableEditorPanel(this.project))
    group.addEditor(ExecutionBundle.message("logs.tab.title", *arrayOfNulls(0)), LogConfigurationPanel())
    return group
  }

  override fun readExternal(element: Element) {
    super.readExternal(element)
    JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element)
    XmlSerializer.deserializeInto(this, element)
    XmlSerializer.deserializeInto(myData, element)

    readModule(element)
    EnvironmentVariablesComponent.readExternal(element, envs)
  }

  override fun writeExternal(element: Element) {
    super.writeExternal(element)
    JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element)
    XmlSerializer.serializeInto(this, element)
    XmlSerializer.serializeInto(myData, element)
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
    var contractFile: String? = null
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
    var ajreEnabled = false
    @JvmField
    var ajre: String? = null

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

    fun setContract(contract: SolContractDefinition) {
      this.contractName = contract.name
      this.contractFile = contract.containingFile.virtualFile.path
    }

    fun setFunction(methodLocation: SolFunctionDefinition) {
      setContract(methodLocation.contract)
      functionName = methodLocation.name
    }
  }

  override fun checkConfiguration() {
    if (!SoliditySettings.instance.useSolcJ || !SoliditySettings.instance.validateEvm()) {
      throw RuntimeConfigurationError("EVM is not configured", SoliditySettingsConfigurable(SoliditySettings.instance).getQuickFix(project))
    }
    if (configurationModule.module == null) {
      throw RuntimeConfigurationError("Module is not specified")
    }
    JavaParametersUtil.checkAlternativeJRE(this)
    val configurationModule = configurationModule
    val psiContract = SearchUtils.findContract(myData.getGetContractName(), configurationModule.project, configurationModule.module)
      ?: throw RuntimeConfigurationError("Can't find contract ${myData.contractName} within module ${configurationModule.moduleName}")
    if (psiContract.containingFile.virtualFile.path != myData.contractFile) {
      throw RuntimeConfigurationError("Can't find contract ${myData.contractName} within file ${myData.contractFile}")
    }
    if (myData.functionName != null && psiContract.functionDefinitionList.none { it.name == myData.functionName }) {
      throw RuntimeConfigurationError("Can't find method '${myData.functionName}' within contract '${psiContract.name}'")
    }
    ProgramParametersUtil.checkWorkingDirectoryExist(this, project, configurationModule.module)
    JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this)
  }
}
