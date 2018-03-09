package me.serce.solidity.ide.run.compile

import com.intellij.openapi.compiler.*
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.lang.SolidityFileType
import org.ethereum.solidity.compiler.SolidityCompiler
import java.io.DataInput

class SolidityIdeCompiler : SourceInstrumentingCompiler {
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
    val sc = SolidityCompiler(null)
    if (items == null) {
      return FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY;
    }
    items.forEach {

    }
//    val contractFile = File(myConfiguration.getPersistentData().contractFile).absolutePath
//    val compileSrc = sc.compileSrc(File(contractFile), false, true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN)
//    if (compileSrc.isFailed) {
//      throw ExecutionException(compileSrc.errors)
//    }
//    val write = Files.write(Paths.get(outputPath, mainContract), compileSrc.output.toByteArray())
    return arrayOf()
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return true
  }

  override fun getDescription(): String {
    return "Solidity Language Compiler (Solc)"
  }
}
