package me.serce.solidity.lang.types

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.ResourceUtil
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.*

class SolGlobals(
  val file: PsiFile,
  val scope: SolContract,
  val addressScope: SolContract,
  val arrayScope: SolContract,
  val functions: List<SolFunctionDefinition>,
  val members: List<SolStateVariableDeclaration>,
  val types: Map<String, SolContract>
)

class SolInternalTypeFactory(project: Project) {

  companion object {
    fun of(project: Project): SolInternalTypeFactory {
      return ServiceManager.getService(project, SolInternalTypeFactory::class.java)
    }
  }

  val globals by lazy {
    val url = ResourceUtil.getResource(this.javaClass.classLoader, "builtins", "solidity-0.8.15.sol")
    val vf = VfsUtil.findFileByURL(url) ?: error("Could not find builtins virtual file!")
    val file = PsiManager.getInstance(project).findFile(vf) ?: error("Could not find builtins psi file!")
    val solFile = SolidityFile(file.viewProvider)
    val contracts = solFile.findChildrenByClass(SolContractDefinition::class.java)
    val globalsContract = SolContract(contracts.find { it.identifier?.text == "Globals" } ?: error("Could not find Globals contract in builtins file!"))
    val types = contracts.filterNot { it.identifier?.text == "Globals" }.associateBy { it.identifier?.text ?: "" }.mapValues { (_, contract) -> SolContract(contract) }

    SolGlobals(
      file,
      globalsContract,
      SolContract(contracts.find { it.name == "Address" } ?: error("Could not find Address contract in builtins file!")),
      SolContract(contracts.find { it.name == "Array" } ?: error("Could not find Array contract in builtins file!")),
      globalsContract.getMembers(project).filterIsInstance<SolFunctionDefinition>(),
      globalsContract.getMembers(project).filterIsInstance<SolStateVariableDeclaration>(),
      types,
    )
  }
}

fun PsiElement.isGlobal(): Boolean {
  return this.containingFile == SolInternalTypeFactory.of(this.project).globals.file
}
