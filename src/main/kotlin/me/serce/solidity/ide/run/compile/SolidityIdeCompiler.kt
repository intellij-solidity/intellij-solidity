package me.serce.solidity.ide.run.compile

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.*
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import me.serce.solidity.lang.SolidityFileType
import java.io.DataInput
import java.io.File
import java.net.URLClassLoader

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
    val fileByModule = items
      .map { Pair(it, modules.find { m -> m.moduleScope.contains(it.file) } ?: return@map null) }
      .filterNotNull()
      .groupBy { it.second }

    val compiled = fileByModule.map {
      Pair(it.value, Solc.compile(it.value.map { File(it.first.file.path) }, File(CompilerPaths.getModuleOutputDirectory(it.key, false)!!.path)))
    }.groupBy { it.second.success }

    compiled[false]?.forEach {
      it.second.messages.split("\n").forEach {
        context.addMessage(CompilerMessageCategory.ERROR, it, null, -1, -1) }
    }
    val success = compiled[true] ?: return emptyArray()
    return success.flatMap { it.first }.map { it.first }.toTypedArray()
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return solcBridge != null
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}

