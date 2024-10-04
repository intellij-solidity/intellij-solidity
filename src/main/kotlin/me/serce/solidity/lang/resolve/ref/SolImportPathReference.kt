package me.serce.solidity.lang.resolve.ref

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.tree.LeafElement
import me.serce.solidity.ide.inspections.fixes.ImportFileAction
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.impl.SolImportPathElement
import org.toml.lang.psi.TomlArray
import org.toml.lang.psi.TomlKeyValueOwner
import java.nio.file.Paths

class SolImportPathReference(element: SolImportPathElement) : SolReferenceBase<SolImportPathElement>(element) {
  override fun singleResolve(): PsiElement? {
    val importText = element.text
    if (importText.length < 2) {
      return null
    }
    val path = importText.substring(1, importText.length - 1)
    return findImportFile(element.containingFile.originalFile.virtualFile, path, element.project )
      ?.let { PsiManager.getInstance(element.project).findFile(it) }
  }

  companion object {

    fun findImportFile(file: VirtualFile, path: String, project: Project): VirtualFile? {
      val directFile = file.findFileByRelativePath("../$path")
      return if (directFile != null) {
        directFile
      } else {
        val npmFile = findNpmImportFile(file, path)
        if (npmFile != null) {
          return npmFile
        }
        val ethPmFile = findEthPMImportFile(file, path)
        if (ethPmFile != null) {
          return ethPmFile
        }
        findFoundryImportFile(file, path, project)
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

    // apply foundry remappings to import path
    private fun applyRemappings(remappings: ArrayList<Pair<String, String>>, path: String): String {
      var output = path
      remappings.forEach { (prefix, target) ->
        if (path.contains(prefix)) {
          output = path.replace(prefix, target)
          return output
        }
      }
      return output;
    }

    private fun foundryDefaultFallback(file: VirtualFile, path: String): VirtualFile? {
      val count = Paths.get(path).nameCount
      if (count < 2) {
        return null;
      }
      val libName = Paths.get(path).subpath(0, 1).toString()
      val libFile = Paths.get(path).subpath(1, count).toString()
      val test = file.findFileByRelativePath("lib/$libName/src/$libFile")
      return test;
    }

    // default lib located at: forge-std/Test.sol => lib/forge-std/src/Test.sol
    private fun findFoundryImportFile(file: VirtualFile, path: String, project: Project): VirtualFile? {
      val testRemappingFile = file.findFileByRelativePath("remappings.txt")
      val remappings = arrayListOf<Pair<String, String>>()
      if (testRemappingFile != null) {
        val mappingsContents = testRemappingFile.contentsToByteArray().toString(Charsets.UTF_8).split("[\r\n]+".toRegex());
        mappingsContents.forEach { mapping ->
          val splitMapping = mapping.split("=")
          if (splitMapping.size == 2) {
            remappings.add(Pair(splitMapping[0].trim(), splitMapping[1].trim()))
          }
        }
      }

      remappings += remappingsFromFoundryConfigFile(file, project)

      val remappedPath = applyRemappings(remappings, path)
      val testRemappedPath = file.findFileByRelativePath(remappedPath)
      val testFoundryFallback = foundryDefaultFallback(file, path)

      return when {
        testRemappedPath != null -> testRemappedPath
        testFoundryFallback != null -> testFoundryFallback
        file.parent != null -> findFoundryImportFile(file.parent, path, project)
        else -> null
      }
    }

    private fun remappingsFromFoundryConfigFile(file: VirtualFile, project: Project): List<Pair<String, String>> {
      val foundryConfigFile = file.findFileByRelativePath("foundry.toml")
      val remappings = arrayListOf<Pair<String, String>>()
      if (foundryConfigFile != null) {
        val foundryFile = PsiManager.getInstance(project).findFile(foundryConfigFile)
        foundryFile?.children?.filterIsInstance<TomlKeyValueOwner>()?.flatMap { it.entries }
          ?.find { it.key.segments.firstOrNull()?.name == "remappings" }
          ?.let {
            (it.value as TomlArray).elements.forEach { mapping ->
              val splitMapping = mapping.text.trim('"').split("=")
              if (splitMapping.size == 2) {
                remappings.add(Pair(splitMapping[0].trim(), splitMapping[1].trim()))
              }
            }
          }
      }
      return remappings
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
    val newPath = ImportFileAction.buildImportPath(element.project, this.element.containingFile.virtualFile, file.virtualFile)
    val identifier = this.element.referenceNameElement as? LeafElement ?: return element
    return identifier.replaceWithText("\"$newPath\"").psi ?: element
  }
}
