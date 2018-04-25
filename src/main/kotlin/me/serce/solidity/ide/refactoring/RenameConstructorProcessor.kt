package me.serce.solidity.ide.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import me.serce.solidity.lang.psi.SolFunctionDefinition

class RenameConstructorProcessor : RenamePsiElementProcessor() {
  override fun canProcessElement(element: PsiElement): Boolean {
    return element is SolFunctionDefinition && element.isConstructor
  }

  override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>, scope: SearchScope) {
    allRenames[(element as SolFunctionDefinition).parent] = newName
  }
}
