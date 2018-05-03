package me.serce.solidity.ide.interop

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import org.web3j.codegen.SolidityFunctionWrapper

object Web3jStubGenerator : Sol2JavaGenerator {
  override fun generate(project: Project, dir: VirtualFile, contracts: List<CompiledContractDefinition>) {
    val basePackageName = SoliditySettings.instance.basePackage
    val wrapper = SolidityFunctionWrapper(true)
    contracts.forEach {
      wrapper.generateJavaFiles(it.contract.name, it.metadata.bin, it.metadata.abi, dir.path, basePackageName)
    }
  }

}
