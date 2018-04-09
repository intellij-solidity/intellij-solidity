package me.serce.solidity.ide.interop

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import java.io.File


object DependencyConfigurator {

  private const val libName = "ethereumj"

  init {
    ApplicationManager.getApplication().messageBus.connect().subscribe(SoliditySettingsListener.TOPIC, object : SoliditySettingsListener {
      override fun settingsChanged() {
        if (SoliditySettings.instance.dependenciesAutoRefresh) {
          ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Refresh dependencies") {
            override fun run(indicator: ProgressIndicator) {
              ProjectManager.getInstance().openProjects
                .forEach { project ->
                  refreshProject(project)
                }
            }
          })
        }
      }
    })
    ApplicationManager.getApplication().messageBus.connect().subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectOpened(project: Project?) {
        if (SoliditySettings.instance.dependenciesAutoRefresh) {
          refreshProject(project ?: return)
        }
      }
    })
  }

  private fun refreshProject(project: Project) {
    ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Generating java stubs") {
      override fun run(indicator: ProgressIndicator) {
        updateDepenedencies(project)
      }
    })
  }

  private fun updateDepenedencies(project: Project) {
    val jarFilePath = SoliditySettings.instance.pathToEvm
    ModuleManager.getInstance(project).modules
      .forEach { module ->
        ModuleRootModificationUtil.updateModel(module) {
          val modifiableModel = it.moduleLibraryTable.modifiableModel
          val lib = modifiableModel.getLibraryByName(libName)
          if (lib != null) {
            modifiableModel.removeLibrary(lib)
            modifiableModel.commit()
          }
          if (jarFilePath.isNotEmpty()) {
            attachDirBasedLibrary(jarFilePath, it)
          }
        }
      }
  }

  private fun attachDirBasedLibrary(dir: String, modifiableRootModel: ModifiableRootModel) {
    val isDir = File(dir).isDirectory
    val urlString = if (isDir) VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, dir) else
      VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, dir) + JarFileSystem.JAR_SEPARATOR
    val dirVirtualFile = VirtualFileManager.getInstance().findFileByUrl(urlString) ?: return
    var library = modifiableRootModel.moduleLibraryTable.getLibraryByName(libName)
    if (library == null) {
      library = modifiableRootModel.moduleLibraryTable.createLibrary(libName)
      val libraryModel = library.modifiableModel
      if (isDir) {
        libraryModel.addJarDirectory(dirVirtualFile, true, OrderRootType.CLASSES)
      } else {
        libraryModel.addRoot(dirVirtualFile, OrderRootType.CLASSES)
      }
      libraryModel.commit()
    }
  }
}
