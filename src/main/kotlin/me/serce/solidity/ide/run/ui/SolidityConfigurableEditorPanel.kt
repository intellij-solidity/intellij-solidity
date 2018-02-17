package me.serce.solidity.ide.run.ui

import com.intellij.application.options.ModulesComboBox
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.BrowseModuleValueActionListener
import com.intellij.execution.testframework.SourceScope
import com.intellij.execution.ui.CommonJavaParametersPanel
import com.intellij.execution.ui.ConfigurationModuleSelector
import com.intellij.execution.ui.DefaultJreSelector
import com.intellij.execution.ui.JrePathEditor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.ui.EditorTextField
import com.intellij.ui.PanelWithAnchor
import com.intellij.util.ui.UIUtil
import me.serce.solidity.ide.run.*
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel


class SolidityConfigurableEditorPanel(private val myProject: Project) : SettingsEditor<SolidityRunConfig>(), PanelWithAnchor {

  val moduleSelector: ConfigurationModuleSelector
  private val myContractLocations = arrayOfNulls<LabeledComponent<EditorTextFieldWithBrowseButton>>(2)
  private val myModel: SolidityConfigurationModel
  private val myBrowsers: Array<BrowseModuleValueActionListener<JComponent>>
  private var myContract: LabeledComponent<EditorTextFieldWithBrowseButton>? = null
  private var myFunction: LabeledComponent<EditorTextFieldWithBrowseButton>? = null
  // Fields
  private var myWholePanel: JPanel? = null
  private var myModule: LabeledComponent<ModulesComboBox>? = null
  private var myCommonJavaParameters: CommonJavaParametersPanel? = null
  private var myJrePathEditor: JrePathEditor? = null
  private var anchor: JComponent? = null

  val modulesComponent: ModulesComboBox
    get() = myModule!!.component

  private val contractName: String
    get() =
      getContractLocation(SolidityConfigurationModel.CONTRACT).component.text

  init {
    myModel = SolidityConfigurationModel(myProject)
    moduleSelector = ConfigurationModuleSelector(myProject, modulesComponent)
    myJrePathEditor!!.setDefaultJreSelector(DefaultJreSelector.fromModuleDependencies(modulesComponent, false))
    myCommonJavaParameters!!.setModuleContext(moduleSelector.module)
    myCommonJavaParameters!!.setHasModuleMacro()
    myModule!!.component.addActionListener { _ -> myCommonJavaParameters!!.setModuleContext(moduleSelector.module) }
    myBrowsers = arrayOf(ContractChooserActionListener(myProject), object : FunctionBrowser(myProject) {

      override val contractName: String
        get() = this@SolidityConfigurableEditorPanel.contractName

      override val moduleSelector: ConfigurationModuleSelector
        get() = this@SolidityConfigurableEditorPanel.moduleSelector

      override fun getFilter(contractClass: SolContractDefinition?): Condition<SolFunctionDefinition> {
        return Condition { psiMethod -> SearchUtils.runnableFilter.invoke(psiMethod) }
      }
    }, object : BrowseModuleValueActionListener<JComponent>(myProject) {
      override fun showDialog(): String? {
        val virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), project, null)
        return if (virtualFile != null) {
          FileUtil.toSystemDependentName(virtualFile.path)
        } else null
      }
    })

    myContractLocations[SolidityConfigurationModel.CONTRACT] = myContract
    myContractLocations[SolidityConfigurationModel.FUNCTION] = myFunction

    // Done

    myModel.setListener(this)

    installDocuments()

    UIUtil.setEnabled(myCommonJavaParameters!!.programParametersComponent, false, true)

//    setAnchor(mySearchForContractLabel)
    myJrePathEditor!!.setAnchor(myModule!!.label)
    myCommonJavaParameters!!.setAnchor(myModule!!.label)

    val model = DefaultComboBoxModel<String>()
    model.addElement("All")

    val changeLists = ChangeListManager.getInstance(myProject).changeLists
    for (changeList in changeLists) {
      model.addElement(changeList.name)
    }
  }

  public override fun applyEditorTo(configuration: SolidityRunConfig) {
    myModel.apply(configuration)
    applyHelpersTo(configuration)
    configuration.alternativeJrePath = myJrePathEditor!!.jrePathOrName
    configuration.isAlternativeJrePathEnabled = myJrePathEditor!!.isAlternativeJreSelected

    myCommonJavaParameters!!.applyTo(configuration)
  }

  public override fun resetEditorFrom(configuration: SolidityRunConfig) {
    myModel.reset(configuration)
    myCommonJavaParameters!!.reset(configuration)
    moduleSelector.reset(configuration)
    myJrePathEditor!!
      .setPathOrName(configuration.alternativeJrePath, configuration.isAlternativeJrePathEnabled)
  }

  private fun installDocuments() {
    for (i in myContractLocations.indices) {
      val contractLocation = getContractLocation(i)
      @Suppress("UNCHECKED_CAST")
      val component = contractLocation.component as ComponentWithBrowseButton<JComponent>
      var document: Any
      document = (component.childComponent as EditorTextField).document
      myBrowsers[i].setField(component)
      if (myBrowsers[i] is FunctionBrowser) {
        val childComponent = component.childComponent as EditorTextField
        (myBrowsers[i] as FunctionBrowser).installCompletion(childComponent)
        document = childComponent.document
      }
      myModel.setContractDocument(i, document)
    }
  }

  fun getContractLocation(index: Int): LabeledComponent<EditorTextFieldWithBrowseButton> {
    return myContractLocations[index]!!
  }

  private fun createUIComponents() {
    myContract = LabeledComponent()
    myContract!!.component = EditorTextFieldWithBrowseButton(myProject)

    myFunction = LabeledComponent()
    val textFieldWithBrowseButton = EditorTextFieldWithBrowseButton(myProject)
    myFunction!!.component = textFieldWithBrowseButton
  }

  override fun getAnchor(): JComponent? {
    return anchor
  }

  override fun setAnchor(anchor: JComponent?) {
    this.anchor = anchor
    myContract!!.anchor = anchor
    myFunction!!.anchor = anchor
  }

  public override fun createEditor(): JComponent {
    return myWholePanel!!
  }

  private fun applyHelpersTo(currentState: SolidityRunConfig) {
    myCommonJavaParameters!!.applyTo(currentState)
    moduleSelector.applyTo(currentState)
  }

  private inner class ContractChooserActionListener(project: Project) : ContractClassBrowser(project) {

    @Throws(ContractBrowser.NoFilterException::class)
    override fun filter(): IContractFilter.ContractFilterWithScope {
      try {
        return ContractFilter.create(SourceScope.wholeProject(project))
      } catch (ignore: ContractFilter.NoContractException) {
        throw ContractBrowser.NoFilterException(MessagesEx.MessageInfo(project,
          ignore.message,
          ExecutionBundle.message("cannot.browse.test.inheritors.dialog.title")))
      }

    }
  }

  private open inner class ContractClassBrowser(project: Project) : ContractBrowser(project, ExecutionBundle.message("choose.test.class.dialog.title")) {

    override fun findContract(className: String): SolContractDefinition? {
      return SearchUtils.findContract(className, myProject)
    }

    @Throws(ContractBrowser.NoFilterException::class)
    override fun filter(): IContractFilter.ContractFilterWithScope {
      val contractFilter: IContractFilter.ContractFilterWithScope
      try {
        val configurationCopy = SolidityRunConfig(SolidityRunConfigModule(myProject),
          SolidityConfigurationType.getInstance().configurationFactories[0])
        applyEditorTo(configurationCopy)
        contractFilter = ContractFilter
          .create(SourceScope.modulesWithDependencies(configurationCopy.modules))
      } catch (e: ContractFilter.NoContractException) {
        throw ContractBrowser.NoFilterException(MessagesEx.MessageInfo(project, "Message", "title"))
      }
      return contractFilter
    }
  }
}
