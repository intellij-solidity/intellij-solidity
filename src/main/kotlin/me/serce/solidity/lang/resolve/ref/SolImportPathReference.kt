package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafElement
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.impl.SolImportPathElement

class SolImportPathReference(element: SolImportPathElement) : SolReferenceBase<SolImportPathElement>(element) {
  override fun singleResolve(): PsiElement? {
    val importText = element.text
    if (importText.length < 2) {
      return null
    }
    val path = importText.substring(1, importText.length - 1)
    val file = findImportFile(element.containingFile.virtualFile, path)
    if (file != null) {
      return PsiManager.getInstance(element.project).findFile(file)
    } else {
      return null
    }
  }

  private fun findImportFile(file: VirtualFile, path: String): VirtualFile? {
    val directFile = file.findFileByRelativePath("../$path")
    if (directFile != null) {
      return directFile
    } else {
      return findNpmImportFile(file, path)
    }
  }

  private fun findNpmImportFile(file: VirtualFile, path: String): VirtualFile? {
    val test = file.findFileByRelativePath("node_modules/$path")
    return when {
        test != null -> test
        file.parent != null -> findNpmImportFile(file.parent, path)
        else -> null
    }
  }

  override fun doRename(identifier: PsiElement, newName: String) {
    if (identifier !is LeafElement) {
      return
    }
    val renamedElement = resolve()
    if (renamedElement !is SolidityFile) {
      return
    }
    val name = renamedElement.name
    val currentPath: String? = (identifier as PsiElement).text
    if (currentPath == null) {
      return
    }
    val newImportPath = currentPath.replace(name, newName)
    identifier.replaceWithText(newImportPath)
  }
}
