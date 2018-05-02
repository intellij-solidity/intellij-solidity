package me.serce.solidity.ide.run.compile

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import me.serce.solidity.ide.interop.CompiledContractDefinition
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.run.ContractUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

object SolidityCompiler {
  private const val genDir = "src-gen"

  fun generate(params: SolCompileParams) {
    if (params.stubs) generateSrcDir(params.module, params.project)

    val contracts = collectContracts(params.project, params.contracts)

    contracts.forEach { m, cs ->
      val module = m ?: return@forEach
      processModule(module, params.project, cs, params.notifications, params.stubs)
    }
  }


  private fun collectContracts(project: Project, items: List<VirtualFile>): Map<Module?, List<SolContractDefinition>> {
    val psiManager = PsiManager.getInstance(project)
    return runReadAction {
      items.asSequence()
        .map { psiManager.findFile(it) }
        .filterIsInstance(SolidityFile::class.java)
        .flatMap { it.children.asSequence() }
        .filterIsInstance(SolContractDefinition::class.java)
        .filterNot { it.name.isNullOrBlank() }
        .groupBy {
          projectFileIndex(project).getModuleForFile(it.containingFile.virtualFile)
        }
    }
  }

  fun getOutputDir(module: Module): File {
    var outputPath: VirtualFile? = try {
      Class.forName("com.intellij.openapi.compiler.CompilerPaths")
      getIdeModuleOutputDirectory(module)
    } catch (e: ClassNotFoundException) {
      null
    }

    return if (outputPath != null) File(outputPath.path) else {
      val moduleOutDir = Paths.get(module.project.basePath, "out", "production", module.name).toFile()
      moduleOutDir.mkdirs()
      moduleOutDir
    }
  }

  private fun getIdeModuleOutputDirectory(module: Module) =
    CompilerPaths.getModuleOutputDirectory(module, false)


  private fun processModule(module: Module, project: Project, contracts: List<SolContractDefinition>, notifications: Boolean, stubs: Boolean) {
    val outputDir = getOutputDir(module)

    val sources = runReadAction { contracts.map { File(it.containingFile.virtualFile.canonicalPath) } }
    val solcResult = Solc.compile(sources, outputDir, project.baseDir)
    if (notifications) {
      SolcMessageProcessor.showNotification(solcResult, project)
    }
    if (!solcResult.success || !stubs) return

    val srcRoot = findGenSourceRoot(module) ?: return

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
    VfsUtil.markDirtyAndRefresh(true, true, true, srcRoot)
  }

  private fun generateSrcDir(modules: Array<out Module>, project: Project) {
    modules.forEach {
        WriteCommandAction.runWriteCommandAction(project) {
          val model = ModuleRootManager.getInstance(it).modifiableModel
          val ce = model.contentEntries.first() ?: model.addContentEntry(project.baseDir)
          val genDir = VfsUtil.createDirectoryIfMissing(ce.file, genDir)
          if (ce.sourceFolderFiles.none { it.path == genDir.path }) ce.addSourceFolder(genDir, false)
          model.commit()
        }
      }
  }

  private fun findGenSourceRoot(module: Module): VirtualFile? {
    return ModuleRootManager.getInstance(module).sourceRoots.firstOrNull { it.name == genDir }
  }
}


data class SolCompileParams(
  val module: Array<out Module>,
  val project: Project,
  val contracts: List<VirtualFile>,
  val notifications: Boolean,
  val stubs: Boolean
)

fun projectFileIndex(project: Project) = ProjectFileIndex.getInstance(project)
