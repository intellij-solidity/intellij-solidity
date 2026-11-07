package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.ide.inspections.fixes.AddOverrideFix
import me.serce.solidity.ide.inspections.fixes.SolSpecifierEditor
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.function.SolOverrideResolver

class MissingOverrideInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) = check(o, "Function")
      override fun visitModifierDefinition(o: SolModifierDefinition) = check(o, "Modifier")

      private fun check(member: SolNamedElement, kind: String) {
        val overridden = SolOverrideResolver.directlyOverridden(member)
        if (overridden.isEmpty()) return
        val contract = SolOverrideResolver.containingContract(member) ?: return
        val bases = SolOverrideResolver.visibleDeclaringBases(contract, member)
        val baseNames = bases.mapNotNull { it.name }
        val specifier = SolOverrideResolver.overrideSpecifier(member)
        val fix = AddOverrideFix(member, baseNames)
        when {
          specifier == null -> {
            // since 0.8.8 `override` is optional when overriding a single interface function
            if (bases.size == 1 && bases[0].contractType == ContractType.INTERFACE) return
            val anchor = nameElement(member) ?: return
            holder.registerProblem(
              anchor,
              "$kind overrides '${SolOverrideResolver.label(overridden.first())}' but is missing 'override'",
              ProblemHighlightType.GENERIC_ERROR,
              fix
            )
          }
          bases.size > 1 && specifier.userDefinedTypeNameList.isEmpty() -> holder.registerProblem(
            specifier,
            "Must specify overridden contracts: ${SolSpecifierEditor.overrideText(baseNames)}",
            ProblemHighlightType.GENERIC_ERROR,
            fix
          )
        }
      }
    }
  }

  companion object {
    /** Name identifier, or the `receive`/`fallback` keyword. */
    fun nameElement(member: SolNamedElement): PsiElement? = when (member) {
      is SolFunctionDefinition -> member.identifier ?: member.firstChild
      is SolModifierDefinition -> member.identifier
      else -> null
    }
  }
}
