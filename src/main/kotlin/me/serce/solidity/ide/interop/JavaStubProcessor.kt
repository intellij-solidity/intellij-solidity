package me.serce.solidity.ide.interop

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.compiler.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import me.serce.solidity.ide.run.compile.SolidityIdeCompiler
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolContractDefinition
import java.io.DataInput
import java.nio.file.Files
import java.nio.file.Paths


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
    return SolidityIdeCompiler.collectProcessingItems(context)
  }

  override fun process(context: CompileContext, items: Array<FileProcessingCompiler.ProcessingItem>): Array<FileProcessingCompiler.ProcessingItem> {
    val modules = context.compileScope.affectedModules
    generate(modules, context.project, items.map { it.file })
    return items
  }

  fun generate(modules: Array<out Module>, project: Project, items: List<VirtualFile>) {
    DependencyConfigurator.refreshDependencies(project)
    modules.forEach {
      val moduleDir = it.moduleFile!!.parent
      if (moduleDir.findChild("gen") != null) {
        return@forEach
      }
      ApplicationManager.getApplication().invokeAndWait {
        val childDirectory = moduleDir.createChildDirectory(this, "gen")
        val model = ModuleRootManager.getInstance(it).modifiableModel
        model.addContentEntry(childDirectory).addSourceFolder(childDirectory.createChildDirectory(this, "src"), false)
        model.commit()
      }
    }

    val psiManager = PsiManager.getInstance(project)
    ApplicationManager.getApplication().runReadAction {
      items.asSequence()
        .map { psiManager.findFile(it) }
        .filter { it is SolidityFile }
        .filterNotNull()
        .flatMap { it.children.asSequence() }
        .filter { it is SolContractDefinition }
        .filterNotNull()
        .map { it as SolContractDefinition }
        .map { Pair(it, JavaStubGenerator.convert(it)) }
        .forEach {
          val module = ProjectFileIndex.getInstance(project).getModuleForFile(it.first.containingFile.virtualFile) ?: return@forEach
          val srcRoot = module.moduleFile?.parent?.findChild("gen")?.findChild("src") ?: return@forEach
          if (srcRoot.findChild("stubs") == null) {
            WriteCommandAction.runWriteCommandAction(project, { srcRoot.createChildDirectory(this, "stubs") })
          }
          Files.write(Paths.get(srcRoot.canonicalPath, "stubs", "${it.first.name}.java"), it.second.toByteArray())
        }
      }
  }
}
