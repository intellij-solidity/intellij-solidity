package me.serce.solidity.ide.interop

import com.intellij.compiler.impl.CompilerUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.compiler.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import me.serce.solidity.ide.run.compile.Solc
import me.serce.solidity.ide.run.compile.SolcMessageProcessor
import me.serce.solidity.ide.run.compile.SolidityIdeCompiler
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.run.ContractUtils
import java.io.DataInput
import java.io.File
import java.nio.file.Files
import java.util.stream.Collectors


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
    generate(modules, context.project, items.map { it.file }, false)
    return items
  }

  private const val genDir = "src-gen"

  fun generate(modules: Array<out Module>, project: Project, items: List<VirtualFile>, notifications: Boolean) {
    generateSrcDir(modules, project)

    val contracts = collectContracts(project, items)

    contracts.forEach { m, cs ->
      val module = m ?: return@forEach
      processModule(module, project, cs, notifications)
    }
  }


  private fun collectContracts(project: Project, items: List<VirtualFile>): Map<Module?, List<SolContractDefinition>> {
    val psiManager = PsiManager.getInstance(project)
    return ApplicationManager.getApplication().runReadAction(Computable {
      items.asSequence()
        .map { psiManager.findFile(it) }
        .filterIsInstance(SolidityFile::class.java)
        .flatMap { it.children.asSequence() }
        .filterIsInstance(SolContractDefinition::class.java)
        .filterNot { it.name.isNullOrBlank() }
        .groupBy {
          ProjectFileIndex.getInstance(project).getModuleForFile(it.containingFile.virtualFile)
        }
    })!!
  }

  private fun processModule(module: Module, project: Project, contracts: List<SolContractDefinition>, notifications: Boolean) {
    val srcRoot = findGenSourceRoot(module) ?: return
    val outputDir = SolidityIdeCompiler.getOutputDir(module)

    val solcResult = Solc.compile(contracts.map { File(it.containingFile.virtualFile.canonicalPath) }, outputDir)
    if (notifications) {
      SolcMessageProcessor.showNotification(solcResult, project)
    }
    if (!solcResult.success) return

    val namedContract = contracts.map { it.name to it }.toMap()
    val infos = Files.walk(outputDir.toPath()).map {
      val cm = ContractUtils.readContract(it) ?: return@map null
      val contract = namedContract[FileUtil.getNameWithoutExtension(it.toFile())] ?: return@map null
      CompiledContractDefinition(cm, contract)
    }.filter { it != null }
      .collect(Collectors.toList())
    @Suppress("UNCHECKED_CAST")
    val compiledContracts = infos as List<CompiledContractDefinition>

    SoliditySettings.instance.genStyle.generator.generate(project, srcRoot, compiledContracts)
    CompilerUtil.refreshIODirectories(listOf(File(srcRoot.path)))
  }

  private fun generateSrcDir(modules: Array<out Module>, project: Project) {
    modules.forEach {
      if (findGenSourceRoot(it) != null) {
        return@forEach
      }
      WriteCommandAction.runWriteCommandAction(project) {
        val moduleDir = it.moduleFile!!.parent
        val childDirectory = moduleDir.findChild(genDir) ?: moduleDir.createChildDirectory(this, genDir)
        val model = ModuleRootManager.getInstance(it).modifiableModel
        model.addContentEntry(childDirectory).addSourceFolder(childDirectory, false)
        model.commit()
      }
    }
  }

  private fun findGenSourceRoot(module: Module): VirtualFile? {
    return ModuleRootManager.getInstance(module).sourceRoots.firstOrNull { it.name == genDir }
  }


}
