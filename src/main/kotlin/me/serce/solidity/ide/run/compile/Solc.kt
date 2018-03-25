package me.serce.solidity.ide.run.compile

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.StreamUtil
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

object Solc  {
  private var solcExecutable : File? = null

  init {
      ApplicationManager.getApplication().messageBus.connect().subscribe(SoliditySettingsListener.TOPIC, object : SoliditySettingsListener {
        override fun settingsChanged() {
          updateSolcExecutable()
        }
      })
    updateSolcExecutable()
  }

  private fun updateSolcExecutable() {
    val evm = SoliditySettings.instance.pathToEvm
    solcExecutable = if (!evm.isNullOrBlank()) {
      val classLoader = URLClassLoader(SoliditySettings.getUrls(evm).map { it.toUri().toURL() }.toTypedArray())
      extractSolc(classLoader)
    } else null
  }

  private fun extractSolc(classLoader: URLClassLoader): File {
    val os = when  {
      SystemInfoRt.isLinux -> "linux"
      SystemInfoRt.isWindows -> "win"
      SystemInfoRt.isMac -> "mac"
      else -> {
        throw IllegalStateException("Unknown OS name: ${SystemInfoRt.OS_NAME}")
      }
    }
    val solcResDir = "/native/$os/solc"
    val tmpDir = File(System.getProperty("java.io.tmpdir"), "solc")
    tmpDir.mkdirs()

    val someClass = classLoader.loadClass("org.ethereum.solidity.compiler.SourceArtifact")
    val fileList = StreamUtil.readText(someClass.getResourceAsStream("$solcResDir/file.list"), Charset.defaultCharset())
    val files = fileList.split("\n")
      .map { it.trim() }
      .map {
        val dest = File(tmpDir, it)
        Files.copy(someClass.getResourceAsStream("$solcResDir/$it"), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        dest
      }

    val solcExec = files.first()
    solcExec.setExecutable(true)
    return solcExec
  }

  fun isEnabled() : Boolean {
    return solcExecutable != null
  }

  fun compile(sources : List<File>, output : File) : SolcResult {
    val solc = solcExecutable
    if (solc == null) {
      throw IllegalStateException("No solc instance was found")
    }
    val pb = ProcessBuilder(arrayListOf(solc.canonicalPath, "--abi", "--bin", "-o", output.absolutePath) + sources.map { it.absolutePath })
    pb
      .directory(solc.parentFile)
      .environment().put("LD_LIBRARY_PATH", solc.parentFile.canonicalPath)
    val start = pb.start()
    if (!start.waitFor(30, TimeUnit.SECONDS)) {
      return SolcResult(false, "Failed to wait for compilation in 30 seconds")
    }
    if (start.exitValue() == 0) {
      return SolcResult(true, StreamUtil.readText(start.inputStream, Charset.defaultCharset()))
    } else {
      return SolcResult(false, StreamUtil.readText(start.errorStream, Charset.defaultCharset()))
    }
  }
}

class SolcResult(val success : Boolean, val messages : String )
