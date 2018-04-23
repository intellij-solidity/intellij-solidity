package me.serce.solidity.ide.interop

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.run.SolContractMetadata

enum class Sol2JavaGenerationStyle(val generator: Sol2JavaGenerator) {
  WEB3J(Web3jStubGenerator), ETHJ(EthJStubGenerator)
}

interface Sol2JavaGenerator {
  fun generate(project: Project, dir: VirtualFile, contracts: List<CompiledContractDefinition>)
}

data class CompiledContractDefinition(val metadata: SolContractMetadata, val contract: SolContractDefinition)
