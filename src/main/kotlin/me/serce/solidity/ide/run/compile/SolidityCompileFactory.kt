package me.serce.solidity.ide.run.compile

import com.intellij.openapi.compiler.Compiler
import com.intellij.openapi.compiler.CompilerFactory
import com.intellij.openapi.compiler.CompilerManager
import me.serce.solidity.ide.interop.DependencyConfigurator
import me.serce.solidity.ide.interop.JavaStubProcessor
import me.serce.solidity.lang.SolidityFileType

class SolidityCompileFactory : CompilerFactory {
  override fun createCompilers(compilerManager: CompilerManager): Array<Compiler> {
    compilerManager.addCompilableFileType(SolidityFileType)
    DependencyConfigurator.toString()
    return arrayOf(SolidityIdeCompiler, JavaStubProcessor);
  }
}
