package me.serce.solidity.ide.run.compile

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.compiler.*
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.SolidityFileType
import org.ethereum.solidity.compiler.SolidityCompiler
import java.io.DataInput
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths

object SolidityIdeCompiler : SourceInstrumentingCompiler {
  var cl : ClassLoader? = null

  init {
    val evm = SoliditySettings.instance.pathToEvm
    if (!evm.isNullOrBlank()) {
      cl = URLClassLoader(SoliditySettings.getUrls(evm).map { it.toUri().toURL() }.toTypedArray())
    }
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
    val scope  = context?.compileScope ?: return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
    val files = scope.getFiles(SolidityFileType, true).filterNotNull()
    return files.map { MyProcessingItem(TimestampValidityState(it.modificationStamp), it.canonicalFile!!) }.toTypedArray()
  }

  override fun process(context: CompileContext?, items: Array<out FileProcessingCompiler.ProcessingItem>?): Array<FileProcessingCompiler.ProcessingItem> {
    val prevCl = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = cl
    try {
      if (items == null) {1
        return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY;
      }
      val sc = SolidityCompiler(null)
      val output = context!!.compileScope.affectedModules
      return items.filter {
        val contractFile = it.file
        val module = output.find { it.moduleScope.contains(contractFile) } ?: return@filter false

        val compileSrc = sc.compileSrc(File(contractFile.canonicalPath), false, true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN)
        if (compileSrc.isFailed) {
          context.addMessage(CompilerMessageCategory.ERROR, compileSrc.errors, null, -1, -1)
        }
        Files.write(Paths.get(CompilerPaths.getModuleOutputPath(module, false), contractFile.name), compileSrc.output.toByteArray())
        return@filter true;
      }.toTypedArray()

    } finally {
      Thread.currentThread().contextClassLoader = prevCl
    }
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    if (cl == null) return false
    try {
      Class.forName("org.ethereum.solidity.compiler.SolidityCompiler", false, cl)
      return true
    } catch (e: ClassNotFoundException) {
      return false
    }
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}
