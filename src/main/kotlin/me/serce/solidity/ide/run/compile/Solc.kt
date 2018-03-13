package me.serce.solidity.ide.run.compile

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.StreamUtil
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset

class Solc private constructor() {
  private var solcExecutable : File? = null

  init {
      ApplicationManager.getApplication().messageBus.connect().subscribe(SoliditySettingsListener.TOPIC, object : SoliditySettingsListener {
        override fun settingsChanged() {
          updateSolcBridge()
        }
      })
    updateSolcBridge()
  }

  private fun updateSolcBridge() {
    val evm = SoliditySettings.instance.pathToEvm
    solcExecutable = if (!evm.isNullOrBlank()) {
      val classLoader = URLClassLoader(SoliditySettings.getUrls(evm).map { it.toUri().toURL() }.toTypedArray())
      extractSolc(classLoader)
    } else null
  }

  private fun extractSolc(classLoader: URLClassLoader): File? {
    val os = when  {
      SystemInfoRt.isLinux -> "linux"
      SystemInfoRt.isWindows -> "win"
      SystemInfoRt.isMac -> "mac"
      else -> {
        throw IllegalStateException("Unknown OS name: ${SystemInfoRt.OS_NAME}")
      }
    }
    val readText = StreamUtil.readText(classLoader.getResourceAsStream("/native/$os/solc/file.list"), Charset.defaultCharset())
    return null
  }

  fun compile(sources : List<File>, output : File) : List<String> {
    val pb = ProcessBuilder()
    return arrayListOf()
  }

  companion object {
      fun getInstance() : Solc {
        return Solc()
      }
  }
}
