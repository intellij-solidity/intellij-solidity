package me.serce.solidity.ide.run.compile

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.FileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.lang.SolidityFileType
import java.util.*

class SolCompileAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (SoliditySettings.instance.pathToEvm.isEmpty()) {
      SolcMessageProcessor.showNoEvmMessage(project)
      return
    }
    ProgressManager.getInstance().run(object : Task.Backgroundable(e.project, "Compiling solidity") {
      override fun run(indicator: ProgressIndicator) {
        ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val files = ArrayList<VirtualFile>()
        fileIndex.iterateContent(SolFileIterator(fileIndex, files))
        SolidityCompiler.generate(SolCompileParams(
          ModuleManager.getInstance(project).modules,
          project,
          files,
          notifications = true,
          stubs = SoliditySettings.instance.generateJavaStubs
        ))
      }
    })
  }
}

class SolFileIterator(private val myFileIndex: FileIndex, private val myFiles: MutableCollection<VirtualFile>) : ContentIterator {
  override fun processFile(fileOrDir: VirtualFile): Boolean {
    if (!fileOrDir.isDirectory &&
      fileOrDir.isInLocalFileSystem &&
      SolidityFileType === fileOrDir.fileType
//      &&
//      myFileIndex.isInSourceContent(fileOrDir)
    ) {
      myFiles.add(fileOrDir)
    }

    return true
  }
}
