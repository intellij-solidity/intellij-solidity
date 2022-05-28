package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafElement
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.impl.SolImportPathElement
import java.nio.file.Paths;

class SolImportPathReference(element: SolImportPathElement) : SolReferenceBase<SolImportPathElement>(element) {
  override fun singleResolve(): PsiElement? {
    val importText = element.text
    if (importText.length < 2) {
      return null
    }
    val path = importText.substring(1, importText.length - 1)
    return findImportFile(element.containingFile.originalFile.virtualFile, path)
      ?.let { PsiManager.getInstance(element.project).findFile(it) }
  }

  private fun findImportFile(file: VirtualFile, path: String): VirtualFile? {
    val directFile = file.findFileByRelativePath("../$path")
    return if (directFile != null) {
      directFile
    } else {
      val npmFile = findNpmImportFile(file, path)
      val ethPmFile = findEthPMImportFile(file, path)
      when {
        npmFile != null -> npmFile
        else -> when {
          ethPmFile != null -> ethPmFile
          else -> findFoundryImportFile(file, path)
        }
      }
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

  // default lib located at: forge-std/Test.sol => lib/forge-std/src/Test.sol
  private fun findFoundryImportFile(file: VirtualFile, path: String): VirtualFile? {
    val count = Paths.get(path).nameCount;
    if (count<2) {
      return null;
    }
    val libName = Paths.get(path).subpath(0,1).toString();
    val libFile = Paths.get(path).subpath(1,count).toString();
    val test = file.findFileByRelativePath("lib/$libName/src/$libFile");
    return when {
      test != null -> test
      file.parent != null -> findFoundryImportFile(file.parent, path)
      else -> null
    }
  }

  private fun findEthPMImportFile(file: VirtualFile, path: String): VirtualFile? {
    val test = file.findFileByRelativePath(
      "installed_contracts/" + path.replaceFirst("/", "/contracts/")
    )
    return when {
      test != null -> test
      file.parent != null -> findEthPMImportFile(file.parent, path)
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
