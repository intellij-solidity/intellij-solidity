package me.serce.solidity.lang.resolve.ref

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import me.serce.solidity.lang.psi.impl.SolidityImportPathElement

class SolidityImportPathReference(element: SolidityImportPathElement) : SolidityReferenceBase<SolidityImportPathElement>(element) {
  override fun singleResolve(): PsiElement? {
    val importText = element.text
    if (importText.length < 2) {
      return null
    }
    val path = importText.substring(1, importText.length - 1)
    val file = element.containingFile.virtualFile.findFileByRelativePath("../$path")
    if (file == null) {
      return null
    }
    return PsiManager.getInstance(element.project).findFile(file)
  }
}
