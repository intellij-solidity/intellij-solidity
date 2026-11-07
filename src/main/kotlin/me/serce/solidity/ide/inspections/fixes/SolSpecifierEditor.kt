package me.serce.solidity.ide.inspections.fixes

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolModifierDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.resolve.function.SolOverrideResolver

/**
 * Adds or rewrites `virtual` / `override` specifiers on functions and modifiers.
 *
 * Edits go through the document with exact spacing: when an external formatter (Foundry) or none is configured,
 * IntelliJ's formatter is disabled for Solidity and would not clean up whitespace after PSI insertions.
 */
object SolSpecifierEditor {

  fun overrideText(bases: List<String>) = if (bases.size > 1) "override(${bases.joinToString(", ")})" else "override"

  fun setOverride(project: Project, member: SolNamedElement, bases: List<String>) {
    val text = overrideText(bases)
    val existing = SolOverrideResolver.overrideSpecifier(member)
    if (existing != null) {
      val range = existing.textRange
      edit(project, member) { it.replaceString(range.startOffset, range.endOffset, text) }
    } else {
      val offset = anchor(member)?.textRange?.endOffset ?: return
      edit(project, member) { it.insertString(offset, " $text") }
    }
  }

  fun addVirtual(project: Project, member: SolNamedElement) {
    if (SolOverrideResolver.virtualSpecifier(member) != null) return
    val override = SolOverrideResolver.overrideSpecifier(member)
    if (override != null) {
      val offset = override.textRange.startOffset
      edit(project, member) { it.insertString(offset, "virtual ") }
    } else {
      val offset = anchor(member)?.textRange?.endOffset ?: return
      edit(project, member) { it.insertString(offset, " virtual") }
    }
  }

  /** Element after which a new specifier goes: the last visibility/mutability/virtual/override specifier, else the parameters. */
  private fun anchor(member: SolNamedElement): PsiElement? = when (member) {
    is SolFunctionDefinition ->
      (member.functionVisibilitySpecifierList + member.stateMutabilitySpecifierList).maxByOrNull { it.textOffset }
        ?: member.parameterListList.firstOrNull()
    is SolModifierDefinition ->
      (member.virtualSpecifierList + member.overrideSpecifierList).maxByOrNull { it.textOffset }
        ?: member.parameterList
        ?: member.identifier
    else -> null
  }

  private fun edit(project: Project, member: SolNamedElement, action: (Document) -> Unit) {
    val documentManager = PsiDocumentManager.getInstance(project)
    val document = documentManager.getDocument(member.containingFile) ?: return
    documentManager.doPostponedOperationsAndUnblockDocument(document)
    action(document)
    documentManager.commitDocument(document)
  }
}
