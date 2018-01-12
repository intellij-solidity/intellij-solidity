package me.serce.solidity.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import me.serce.solidity.lang.psi.SolContractDefinition

class RenameContractProcessor: RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean {
    return element is SolContractDefinition
  }

  override fun prepareRenaming(element: PsiElement?, newName: String?, allRenames: MutableMap<PsiElement, String>?, scope: SearchScope?) {
    (element as SolContractDefinition).functionDefinitionList
      .filter { it.isConstructor }
      .forEach { allRenames?.put(it, newName!!) }
  }
}
