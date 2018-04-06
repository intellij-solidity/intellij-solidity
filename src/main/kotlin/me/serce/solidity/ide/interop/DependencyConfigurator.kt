package me.serce.solidity.ide.interop

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.JavadocOrderRootType
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import me.serce.solidity.ide.settings.SoliditySettings
import me.serce.solidity.ide.settings.SoliditySettingsListener
import java.io.File


object DependencyConfigurator {

  private var isDirty = true

  private val libName = "ejvm"

  init {
    ApplicationManager.getApplication().messageBus.connect().subscribe(SoliditySettingsListener.TOPIC, object : SoliditySettingsListener {
      override fun settingsChanged() {
        isDirty = true
      }
    })
  }

  fun refreshDependencies(project: Project) {
    if (isDirty) {
      val jarFilePath = SoliditySettings.instance.pathToEvm
      if (jarFilePath != null) {
        val isDir = File(jarFilePath).isDirectory
          ModuleManager.getInstance(project).modules.forEach {
            if (isDir) attachDirBasedLibrary(it, libName, jarFilePath) else attachJarLibrary(it, libName, jarFilePath)
        }
      }
      isDirty = false
    }
  }

  private fun attachDirBasedLibrary(module: Module, libName: String, dir: String) {
    ApplicationManager.getApplication().runWriteAction {
      val rootManager = ModuleRootManager.getInstance(module)
      val urlString = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, dir)
      val dirVirtualFile = VirtualFileManager.getInstance().findFileByUrl(urlString)
      if (dirVirtualFile != null) {
        val modifiableModel = rootManager.modifiableModel
        val newLib = createDirLib(libName, dirVirtualFile, modifiableModel.moduleLibraryTable)
        modifiableModel.commit()
      }
    }
  }


  private fun createDirLib(libName: String,
                           dirVirtualFile: VirtualFile,
                           table: LibraryTable): Library {
    var library = table.getLibraryByName(libName)
    if (library == null) {
      library = table.createLibrary(libName)

      val libraryModel = library!!.modifiableModel
      libraryModel.addJarDirectory(dirVirtualFile, true, OrderRootType.CLASSES)
      libraryModel.addJarDirectory(dirVirtualFile, true, OrderRootType.SOURCES)
      libraryModel.addJarDirectory(dirVirtualFile, true, JavadocOrderRootType.getInstance())
      libraryModel.commit()
    }
    return library
  }

  private fun attachJarLibrary( module: Module,
                                libName: String,
                                jarFilePath: String,
                                srcFilePath: String? = null,
                                docFilePath: String? = null) {
    ApplicationManager.getApplication().runWriteAction {
      val rootManager = ModuleRootManager.getInstance(module)

      val clzUrlString = VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, jarFilePath) + JarFileSystem.JAR_SEPARATOR
      val srcUrlString = if (srcFilePath != null) VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, srcFilePath) + JarFileSystem.JAR_SEPARATOR else null
      val docUrlString = if (docFilePath != null) VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, docFilePath) + JarFileSystem.JAR_SEPARATOR else null

      val jarVirtualFile = findFile(clzUrlString)
      val srcVirtualFile = findFile(srcUrlString)
      val docVirtualFile = findFile(docUrlString)

      val modifiableModel = rootManager.modifiableModel
      val newLib = createJarLib(libName, modifiableModel.moduleLibraryTable, jarVirtualFile, srcVirtualFile, docVirtualFile)
      modifiableModel.commit()
    }
  }

  private fun findFile(urlString: String?) = if (urlString == null) null else VirtualFileManager.getInstance().findFileByUrl(urlString)

  private fun createJarLib(libName: String,
                           table: LibraryTable,
                           jarVirtualFile: VirtualFile?,
                           srcVirtualFile: VirtualFile?,
                           docVirtualFile: VirtualFile?): Library {
    var library = table.getLibraryByName(libName)
    if (library == null) {
      library = table.createLibrary(libName)
      val libraryModel = library!!.modifiableModel

      if (jarVirtualFile != null) {
        libraryModel.addRoot(jarVirtualFile, OrderRootType.CLASSES)
      }

      if (srcVirtualFile != null) {
        libraryModel.addRoot(srcVirtualFile, OrderRootType.SOURCES)
      }

      if (docVirtualFile != null) {
        libraryModel.addRoot(docVirtualFile, JavadocOrderRootType.getInstance())
      }

      libraryModel.commit()
    }
    return library
  }
}
