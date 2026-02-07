package me.serce.solidity.ide.formatting

import com.intellij.application.options.CodeStyle
import com.intellij.lang.ImportOptimizer
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.descendantsOfType
import me.serce.solidity.ide.inspections.fixes.ImportFileAction
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.outerContract

class SolImportOptimizer : ImportOptimizer {
    override fun supports(file: PsiFile) = file.fileType == SolidityFileType

    override fun processFile(file: PsiFile): Runnable {
        return processFile(file, true)
    }

  fun processFile(file: PsiFile, fullOptimization: Boolean): Runnable {
    val list = file.descendantsOfType<SolImportDirective>().toList().takeIf { it.isNotEmpty() }
    return when {
      list == null || !fullOptimization && list.size <= 1 -> Runnable {}
      fullOptimization -> processFull(file, list)
      else -> processLight(file, list)
    }
  }

  private fun processFull(file: PsiFile, list: List<SolImportDirective>): Runnable {
    val addSpecificSymbols = CodeStyle.getSettings(file).solidityCustomSettings.specificSymbolImports
    val allTypes = SolResolver.collectImportedNames(file).mapNotNull { it.target.let { if (it is SolUserDefinedTypeName) SolResolver.resolveTypeNameUsingImports(it).firstOrNull() else it } }
    val solFactory = SolPsiFactory(file.project)
    val imports = allTypes
      .mapNotNull {
        it.outerContract()?.let { parentContract ->
          if (it is SolFunctionDefinition) return@mapNotNull null
          return@mapNotNull Pair(it, parentContract.name)
        }
        Pair(it, it.name)
      }
      .groupBy { it.first.containingFile }
      .mapNotNull {
        val file1 = it.key.containingFile?.virtualFile ?: return@mapNotNull null
        val solUserDefinedTypeName = if (addSpecificSymbols) it.value.mapNotNull { it.second }.distinct() else emptyList()
        val to = file.virtualFile
        ImportFileAction.createImport(solFactory, solUserDefinedTypeName, file1, to)
      }
      .sortedBy { it.importPath?.text }

    return Runnable {
      extracted(list, imports)
    }
  }

  private fun extracted(list: List<SolImportDirective>, imports: List<SolImportDirective>) {
    val first = list.first().containingFile.let { it.children.filterIsInstance<SolPragmaDirective>().firstOrNull() ?: it.firstChild }
    val parent = first.parent
    list.forEach { it.delete() }
    imports.reversed().forEach { parent.addAfter(it, first) }
    parent.childrenOfType<SolImportDirective>().reversed().filter { SolResolver.collectUsedElements(it).isEmpty() }.forEach { it.delete() }
  }

  private fun processLight(file: PsiFile, list: List<SolImportDirective>): Runnable {
        val factory = PsiFileFactory.getInstance(file.project)

        val sorted = list.sortedBy { it.text }.zip(list).map {
            if (it.first != it.second) {
                factory.createFileFromText("DUMMY.tsol", SolidityFileType, it.first.text).childOfType() ?: it.first
            } else it.first
        }
        return Runnable {
            list.zip(sorted).forEach {
                if (it.first != it.second) {
                    it.first.replace(it.second)
                }
            }
        }
    }
}
