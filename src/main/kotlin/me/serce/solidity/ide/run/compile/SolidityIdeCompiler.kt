package me.serce.solidity.ide.run.compile

import com.intellij.openapi.compiler.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.lang.SolidityFileType
import java.io.DataInput
import java.io.File
import java.nio.file.Paths

object SolidityIdeCompiler : Validator {
// extends Validator to be run after the regular (javac) compiler,
// otherwise the last one will erase all the compiled contract files

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
    if (!Solc.isEnabled()) {
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
      Pair(it.value, Solc.compile(it.value.map { File(it.first.file.path) }, getOutputDir(it.key)))
    }.onEach { SolcMessageProcessor.process(it.second.messages, context) }
      .groupBy { it.second.success }

    val success = compiled[true] ?: return emptyArray()
    return success.flatMap { it.first }.map { it.first }.toTypedArray()
  }

  fun getOutputDir(module: Module): File {
    val outputPath = CompilerPaths.getModuleOutputDirectory(module, false)

    val outputDir = if (outputPath != null) File(outputPath.path) else {
      // todo workaround - fix it properly
      val moduleOutDir = Paths.get(module.project.basePath, "out", "production", module.name).toFile()
      moduleOutDir.mkdirs()
      moduleOutDir
    }
    return outputDir
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return Solc.isEnabled()
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}

