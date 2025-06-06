package me.serce.solidity.ide.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.openapi.util.WriteExternalException
import com.intellij.util.xmlb.Constants
import org.jdom.Element

class ForgeTestRunConfiguration(
  project: Project,
  factory: ForgeTestRunConfigurationFactory,
  name: String
) : LocatableConfigurationBase<ForgeTestCommandLineState>(project, factory, name) {

  var contractName: String? = null
  var testName: String? = null
  var workingDirectory: String = project.basePath ?: ""

  override fun getConfigurationEditor() = ForgeTestRunConfigurationEditor()

  override fun getState(executor: Executor, environment: ExecutionEnvironment) =
    ForgeTestCommandLineState(this, environment)

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    super.readExternal(element)
    testName = JDOMExternalizerUtil.getChildrenValueAttributes(element, "testName")[0]
    contractName = JDOMExternalizerUtil.getChildrenValueAttributes(element, "contractName")[0]
    workingDirectory =
      JDOMExternalizerUtil.getChildrenValueAttributes(element, "workingDirectory")[0] ?: workingDirectory
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    super.writeExternal(element)

    testName?.let {
      val tnEle = Element("testName")
      tnEle.setAttribute(Constants.VALUE, it)
      element.addContent(tnEle)
    }

    contractName?.let {
      val cnEle = Element("contractName")
      cnEle.setAttribute(Constants.VALUE, it)
      element.addContent(cnEle)
    }

    val wdEle = Element("workingDirectory")
    wdEle.setAttribute(Constants.VALUE, workingDirectory)
    element.addContent(wdEle)
  }

  override fun suggestedName(): String = "Forge Test${testName?.let { " - $it" } ?: ""}"
}
