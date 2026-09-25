package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafElement
import me.serce.solidity.ide.inspections.fixes.ImportFileAction
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.impl.SolImportPathElement

class SolImportPathReference(element: SolImportPathElement) : SolReferenceBase<SolImportPathElement>(element) {
  override fun singleResolve(): PsiElement? {
    val importText = element.text
    if (importText.length < 2) {
      return null
    }
    val containingFile = element.containingFile?.originalFile?.virtualFile
    if (containingFile == null || !containingFile.isValid) {
      return null
    }
    val path = importText.substring(1, importText.length - 1)
    return findImportFile(element.project, containingFile, path)
      ?.let { PsiManager.getInstance(element.project).findFile(it) }
  }

  companion object {
    fun findImportFile(project: Project, file: VirtualFile, path: String): VirtualFile? {
      if (!file.isValid) {
        return null
      }
      val directFile = file.findFileByRelativePath("../$path")
      val resolved = directFile
        ?: findNpmImportFile(file, path)
        ?: findEthPMImportFile(file, path)
        ?: findFoundryImportFile(project, file, path)
      return resolved?.takeIf { it.isValid }
    }

    private fun findNpmImportFile(file: VirtualFile, path: String): VirtualFile? {
      val test = file.findFileByRelativePath("node_modules/$path")
      val parent = file.parent
      return when {
        test != null -> test
        parent != null && parent.isValid -> findNpmImportFile(parent, path)
        else -> null
      }
    }

    private fun findFoundryImportFile(project: Project, file: VirtualFile, path: String): VirtualFile? {
      return SolImportConfigService.getInstance(project).resolve(path, file)
    }

    private fun findEthPMImportFile(file: VirtualFile, path: String): VirtualFile? {
      val test = file.findFileByRelativePath(
        "installed_contracts/" + path.replaceFirst("/", "/contracts/")
      )
      val parent = file.parent
      return when {
        test != null -> test
        parent != null && parent.isValid -> findEthPMImportFile(parent, path)
        else -> null
      }
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

  override fun bindToElement(element: PsiElement): PsiElement {
    val file = element as? SolidityFile ?: return element
    val newPath =
      ImportFileAction.buildImportPath(element.project, this.element.containingFile.virtualFile, file.virtualFile)
    val identifier = this.element.referenceNameElement as? LeafElement ?: return element
    return identifier.replaceWithText("\"$newPath\"").psi ?: element
  }
}
