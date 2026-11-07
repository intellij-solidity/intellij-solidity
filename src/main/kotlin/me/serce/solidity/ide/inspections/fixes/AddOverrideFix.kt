package me.serce.solidity.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.psi.SolNamedElement

class AddOverrideFix(member: SolNamedElement, private val bases: List<String>) : LocalQuickFixOnPsiElement(member) {
  override fun getText() = "Add ${SolSpecifierEditor.overrideText(bases)}"
  override fun getFamilyName() = "Add override specifier"

  override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
    SolSpecifierEditor.setOverride(project, startElement as SolNamedElement, bases)
  }
}
