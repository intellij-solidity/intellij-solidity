package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.psi.SolNamedElement

/**
 * Adds `virtual` to [decl]. When [label] is given the fix is offered from another element (the override site)
 * and may edit another file.
 */
class AddVirtualFix(decl: SolNamedElement, private val label: String? = null) : LocalQuickFixOnPsiElement(decl) {
  override fun getText() = if (label == null) "Add virtual" else "Make '$label' virtual"
  override fun getFamilyName() = "Add virtual specifier"

  override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
    SolSpecifierEditor.addVirtual(project, startElement as SolNamedElement)
  }

  // the declaration may live in another file than the one the problem is reported in
  override fun getElementToMakeWritable(currentFile: PsiFile) = startElement?.containingFile ?: currentFile

  companion object {
    /** Whether [decl] lives in an editable project source file. */
    fun canEdit(decl: SolNamedElement): Boolean {
      val vFile = decl.containingFile?.virtualFile ?: return true // in-memory (tests, light files)
      return vFile.isWritable && ProjectFileIndex.getInstance(decl.project).isInContent(vFile)
    }
  }
}
