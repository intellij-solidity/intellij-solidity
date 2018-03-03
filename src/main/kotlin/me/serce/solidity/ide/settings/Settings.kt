package me.serce.solidity.ide.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.util.io.isDirectory
import com.intellij.util.io.isFile
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.Nls
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.swing.JComponent


@State(name = "SoliditySettings", storages = arrayOf(Storage("other.xml")))
class SoliditySettings : PersistentStateComponent<SoliditySettings> {
  var pathToEvm: String? = null
  var pathToDb: String? = null

  override fun getState(): SoliditySettings? {
    return this
  }

  override fun loadState(`object`: SoliditySettings) {
    XmlSerializerUtil.copyBean(`object`, this)
  }

  fun validateEvm(): Boolean {
    return Companion.validateEvm(pathToEvm)
  }

  companion object {

    fun validateEvm(pathToEvm: String?): Boolean {
        return !pathToEvm.isNullOrBlank() && checkJars(pathToEvm!!)
    }

    private fun checkJars(pathToEvm: String): Boolean {
      val p = Paths.get(pathToEvm)

      val files = when  {
        p.isDirectory() -> Files.list(p).collect(Collectors.toList())
        p.isFile() && p.toString().endsWith(".jar") -> listOf(p)
        else -> return false
      }.toMutableList()
      val ethJar = files.indexOfFirst { it.fileName.toString().contains("ethereumj") }
      if (ethJar > 0) {
        files[ethJar] = files[0].also { files[0] = files[ethJar] }
      }
      val cl = URLClassLoader(files.map { it.toUri().toURL() }.toTypedArray())
      try {
        Class.forName("org.ethereum.util.blockchain.StandaloneBlockchain", false, cl)
        return true
      } catch (e: ClassNotFoundException) {
        return false
      }
    }

    val instance: SoliditySettings
      get() = ServiceManager.getService(SoliditySettings::class.java)
  }
}

class SoliditySettingsConfigurable(private val mySettings: SoliditySettings) : SearchableConfigurable, Configurable.NoScroll {
  private var myPanel: SolidityConfigurablePanel? = null

  @Nls
  override fun getDisplayName(): String {
    return "Solidity"
  }

  override fun getHelpTopic(): String {
    return "preferences.Solidity"
  }

  override fun createComponent(): JComponent? {
    myPanel = SolidityConfigurablePanel()
    return myPanel!!.myEvmPathPanel
  }

  override fun isModified(): Boolean {
    return myPanel!!.isModified(mySettings)
  }

  @Throws(ConfigurationException::class)
  override fun apply() {
    myPanel!!.apply(mySettings)
  }

  override fun reset() {
    myPanel!!.reset(mySettings)
  }

  override fun disposeUIResources() {
    myPanel = null
  }

  override fun getId(): String {
    return helpTopic
  }

}


