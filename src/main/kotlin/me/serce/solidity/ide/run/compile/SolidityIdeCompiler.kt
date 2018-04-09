package me.serce.solidity.ide.run.compile

import com.intellij.openapi.compiler.*
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.SolidityFileType
import java.io.DataInput
import java.io.File

object SolidityIdeCompiler : Validator {
// extends Validator to be run after the regular (javac) compiler,
// otherwise the last one will erase all the compiled contract files

  override fun createValidityState(`in`: DataInput?): ValidityState {
    return TimestampValidityState.load(`in`)
  }

  override fun getProcessingItems(context: CompileContext): Array<FileProcessingCompiler.ProcessingItem> {
    if (!SoliditySettings.instance.useSolcJ || !Solc.isEnabled()) {
      return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    }
    return collectProcessingItems(context)
  }

  fun collectProcessingItems(context: CompileContext): Array<FileProcessingCompiler.ProcessingItem> {
    val scope = context.compileScope
    val files = scope.getFiles(SolidityFileType, true).filterNotNull()
    return files.map { SolProcessingItem(TimestampValidityState(it.modificationStamp), it.canonicalFile!!) }.toTypedArray()
  }

  override fun process(context: CompileContext, items: Array<FileProcessingCompiler.ProcessingItem>): Array<FileProcessingCompiler.ProcessingItem> {
    val modules = context.compileScope.affectedModules
    val fileByModule = items
      .map { Pair(it, modules.find { m -> m.moduleScope.contains(it.file) } ?: return@map null) }
      .filterNotNull()
      .groupBy { it.second }

    val compiled = fileByModule.map {
      Pair(it.value, Solc.compile(it.value.map { File(it.first.file.path) }, File(CompilerPaths.getModuleOutputDirectory(it.key, false)!!.path)))
    }.onEach { SolcMessageProcessor.process(it.second.messages, context) }
      .groupBy { it.second.success }

    val success = compiled[true] ?: return emptyArray()
    return success.flatMap { it.first }.map { it.first }.toTypedArray()
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return Solc.isEnabled()
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}

