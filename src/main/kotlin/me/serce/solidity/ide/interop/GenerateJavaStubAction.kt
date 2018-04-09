package me.serce.solidity.ide.interop

import com.intellij.compiler.impl.CompilerContentIterator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.lang.SolidityFileType
import java.util.*

class GenerateJavaStubAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating java stubs") {
      override fun run(indicator: ProgressIndicator) {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val files = ArrayList<VirtualFile>()
        fileIndex.iterateContent(CompilerContentIterator(SolidityFileType, fileIndex, true, files))
        JavaStubProcessor.generate(ModuleManager.getInstance(project).modules, project, files)
      }
    })
  }
}
