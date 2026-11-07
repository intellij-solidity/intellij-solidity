package me.serce.solidity.ide.inspections.fixes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiParserFacade
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.function.SolOverrideResolver

/** PSI edits that add or rewrite `virtual` / `override` specifiers on functions and modifiers. */
object SolSpecifierEditor {

  fun overrideText(bases: List<String>) = if (bases.size > 1) "override(${bases.joinToString(", ")})" else "override"

  fun setOverride(project: Project, member: SolNamedElement, bases: List<String>) {
    val text = overrideText(bases)
    val existing = SolOverrideResolver.overrideSpecifier(member)
    if (existing != null) {
      existing.replace(dummySpecifiers(project, text).single { it.overrideSpecifier != null }.overrideSpecifier!!)
      return
    }
    when (member) {
      is SolFunctionDefinition -> insertAfter(project, member, functionAnchor(member), dummySpecifiers(project, text).single())
      is SolModifierDefinition -> insertAfter(project, member, modifierAnchor(member), dummyModifierSpecifiers(project, text).single())
    }
  }

  fun addVirtual(project: Project, member: SolNamedElement) {
    if (SolOverrideResolver.virtualSpecifier(member) != null) return
    val override = SolOverrideResolver.overrideSpecifier(member)
    when (member) {
      is SolFunctionDefinition -> {
        val spec = dummySpecifiers(project, "virtual").single()
        val overrideWrapper = override?.parent
        if (overrideWrapper != null) insertBefore(project, member, overrideWrapper, spec)
        else insertAfter(project, member, functionAnchor(member), spec)
      }
      is SolModifierDefinition -> {
        val spec = dummyModifierSpecifiers(project, "virtual").single()
        if (override != null) insertBefore(project, member, override, spec)
        else insertAfter(project, member, modifierAnchor(member), spec)
      }
    }
  }

  /** Last visibility/mutability/virtual/override specifier, or the parameter list. */
  private fun functionAnchor(f: SolFunctionDefinition): PsiElement =
    (f.functionVisibilitySpecifierList + f.stateMutabilitySpecifierList).maxByOrNull { it.textOffset }
      ?: f.parameterListList.first()

  private fun modifierAnchor(m: SolModifierDefinition): PsiElement =
    (m.virtualSpecifierList + m.overrideSpecifierList).maxByOrNull { it.textOffset }
      ?: m.parameterList
      ?: m.identifier!!

  private fun insertAfter(project: Project, parent: PsiElement, anchor: PsiElement, element: PsiElement) {
    val added = parent.addAfter(element, anchor)
    parent.addBefore(space(project), added)
  }

  private fun insertBefore(project: Project, parent: PsiElement, anchor: PsiElement, element: PsiElement) {
    val added = parent.addBefore(element, anchor)
    parent.addAfter(space(project), added)
  }

  private fun space(project: Project) = PsiParserFacade.getInstance(project).createWhiteSpaceFromText(" ")

  private fun dummySpecifiers(project: Project, text: String): List<SolFunctionVisibilitySpecifier> =
    SolPsiFactory(project).createFunction("function dummy() $text {}").functionVisibilitySpecifierList

  private fun dummyModifierSpecifiers(project: Project, text: String): List<PsiElement> {
    val modifier = SolPsiFactory(project).createContract("contract Dummy { modifier dummy() $text { _; } }")
      .modifierDefinitionList.single()
    return modifier.virtualSpecifierList + modifier.overrideSpecifierList
  }
}
