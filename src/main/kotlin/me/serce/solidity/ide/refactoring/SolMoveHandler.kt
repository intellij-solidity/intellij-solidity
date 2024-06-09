package me.serce.solidity.ide.refactoring

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesHandler

class SolMoveHandler : MoveFilesOrDirectoriesHandler() {
  override fun doMove(project: Project?, elements: Array<out PsiElement>, targetContainer: PsiElement?, callback: MoveCallback?) {
    val oldRefs = mutableMapOf<String, MutableList<PsiReference>>()
    elements.forEach { oldFile ->
      ReferencesSearch.search(oldFile).forEach {ref ->  oldFile.containingFile?.let { oldRefs.getOrPut(it.name) { mutableListOf() }.add(ref) } }
    }

    super.doMove(project, elements, targetContainer) {
      callback?.refactoringCompleted()
      elements.forEach {newFile ->
        val containingFile: PsiFile? = newFile.containingFile
        if (containingFile != null) {
          oldRefs[containingFile.name]?.forEach { it.bindToElement(newFile) }
        }
      }
    }
  }
}
