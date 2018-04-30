package me.serce.solidity.ide.interop

import com.intellij.openapi.compiler.*
import me.serce.solidity.ide.run.compile.SolCompileParams
import me.serce.solidity.ide.run.compile.Solc
import me.serce.solidity.ide.run.compile.SolidityCompiler
import me.serce.solidity.ide.run.compile.SolidityIdeCompiler
import me.serce.solidity.ide.settings.SoliditySettings
import java.io.DataInput


object JavaStubProcessor : SourceInstrumentingCompiler {

  override fun getDescription(): String {
    return "Solidity-to-Java interoperability stub files generator"
  }

  override fun createValidityState(`in`: DataInput?): ValidityState {
    return TimestampValidityState.load(`in`)
  }

  override fun validateConfiguration(scope: CompileScope?): Boolean {
    return true
  }

  override fun getProcessingItems(context: CompileContext): Array<FileProcessingCompiler.ProcessingItem> {
    return if (SoliditySettings.instance.generateJavaStubs && Solc.isEnabled()) SolidityIdeCompiler.collectProcessingItems(context) else FileProcessingCompiler.ProcessingItem.EMPTY_ARRAY
  }

  override fun process(context: CompileContext, items: Array<FileProcessingCompiler.ProcessingItem>): Array<FileProcessingCompiler.ProcessingItem> {
    val modules = context.compileScope.affectedModules
    SolidityCompiler.generate(SolCompileParams(modules, context.project, items.map { it.file }, notifications = true, stubs = true))
    return items
  }
}

