package me.serce.solidity.ide.run.compile

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.*
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import me.serce.solidity.lang.SolidityFileType
import org.ethereum.solidity.compiler.SolidityCompiler
import java.io.DataInput
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths

object SolidityIdeCompiler : SourceInstrumentingCompiler {
  private var solcBridge: Class<*>? = null

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
    solcBridge = if (!evm.isNullOrBlank()) {
      val cl = URLClassLoader(SoliditySettings.getUrls(evm).map { it.toUri().toURL() }.toTypedArray() + (SolidityIdeCompiler::class.java.classLoader as PluginClassLoader).baseUrls)
      Class.forName("me.serce.solidity.ide.run.compile.SolcBridge", false, cl)
    } else null
  }

  private class MyProcessingItem(val myValidityState: ValidityState, val myFile: VirtualFile) : FileProcessingCompiler.ProcessingItem {
    override fun getValidityState(): ValidityState? {
      return myValidityState
    }

    override fun getFile(): VirtualFile {
      return myFile
    }
  }

  override fun createValidityState(`in`: DataInput?): ValidityState {
    return TimestampValidityState.load(`in`)
  }

  override fun getProcessingItems(context: CompileContext?): Array<FileProcessingCompiler.ProcessingItem> {
    if (solcBridge == null) {
      return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    }
    val scope = context?.compileScope ?: return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    val files = scope.getFiles(SolidityFileType, true).filterNotNull()
    return files.map { MyProcessingItem(TimestampValidityState(it.modificationStamp), it.canonicalFile!!) }.toTypedArray()
  }

  override fun process(context: CompileContext?, items: Array<out FileProcessingCompiler.ProcessingItem>?): Array<FileProcessingCompiler.ProcessingItem> {
    if (items == null) {
      return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    }

    val modules = context!!.compileScope.affectedModules
    val fileWithModule = items
      .map { Pair(it, modules.find { m -> m.moduleScope.contains(it.file) } ?: return@map null) }
      .filterNotNull()
      .toList()

    val sources = fileWithModule.map { File(it.first.file.path) }
    val method = solcBridge!!.getMethod("compile", List::class.java)
    method.isAccessible = true
    @Suppress("unchecked_cast")
    val compile = (method.invoke(null, sources) as List<List<Any>>).map { Pair(it.component1() as Boolean, it.component2() as String) }

    return compile
      .mapIndexed { i, r ->
        if (!r.first) {
          val module = fileWithModule[i].second
          val item = fileWithModule[i].first
          Files.write(Paths.get(CompilerPaths.getModuleOutputPath(module, false), item.file.name), r.second.toByteArray())
          return@mapIndexed item
        } else {
          context.addMessage(CompilerMessageCategory.ERROR, r.second, null, -1, -1)
          return@mapIndexed null
        }
      }
      .filterNotNull()
      .toTypedArray()
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return solcBridge != null
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}

private class SolcBridge {
  companion object {
    val solc = SolidityCompiler(null)
    @JvmStatic
    fun compile(sources: List<File>): List<List<Any>> {
      return sources
        .map { solc.compileSrc(it, false, true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN) }
        .map { listOf(it.isFailed, if (it.isFailed) it.errors else it.output) }
    }
  }
}

