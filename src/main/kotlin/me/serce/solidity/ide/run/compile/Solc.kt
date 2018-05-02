package me.serce.solidity.ide.run.compile

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
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
    solcExecutable = if (!evm.isBlank()) {
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

    // for some reason, we have to load any class to be able to read any resource from the jar
    val someClass = classLoader.loadClass("org.ethereum.util.blockchain.StandaloneBlockchain")

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

  fun compile(sources: List<File>, outputDir: File, baseDir: VirtualFile): SolcResult {
    val solc = solcExecutable ?: throw IllegalStateException("No solc instance was found")
    val pb = ProcessBuilder(arrayListOf(solc.canonicalPath, "--abi", "--bin", "--overwrite", "-o", outputDir.absolutePath) + sources.map {
      Paths.get(baseDir.canonicalPath).relativize(Paths.get(it.path)).toString().replace('\\', '/')
    })
    pb
      .directory(File(baseDir.path))
      .environment().put("LD_LIBRARY_PATH", solc.parentFile.canonicalPath)
    val solcProc = pb.start()
    val outputPromise = runAsync2 { StreamUtil.readText(solcProc.inputStream, Charset.defaultCharset()) }
    val errorPromise = runAsync2 { StreamUtil.readText(solcProc.errorStream, Charset.defaultCharset()) }
    if (!solcProc.waitFor(30, TimeUnit.SECONDS)) {
      solcProc.destroyForcibly()
      return SolcResult(false, "Failed to wait for solc to complete in 30 seconds", -1)
    }
    val output = outputPromise.blockingGet(500)
    val error = errorPromise.blockingGet(500)
    val messages = "$output\n$error"
    val exitValue = solcProc.exitValue()
    return SolcResult(exitValue == 0, messages, exitValue)
  }
}

inline fun <T> runAsync2(crossinline runnable: () -> T): Promise<T> {
  val promise = AsyncPromise<T>()
  AppExecutorUtil.getAppExecutorService().execute {
    val result = try {
      runnable()
    }
    catch (e: Throwable) {
      promise.setError(e)
      return@execute
    }
    promise.setResult(result)
  }
  return promise
}

class SolcResult(val success: Boolean, val messages: String, val exitCode: Int)
