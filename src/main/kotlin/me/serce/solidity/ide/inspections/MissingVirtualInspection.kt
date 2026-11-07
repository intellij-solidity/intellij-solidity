package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.ide.inspections.MissingOverrideInspection.Companion.nameElement
import me.serce.solidity.ide.inspections.fixes.AddVirtualFix
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolModifierDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolVisitor
import me.serce.solidity.lang.resolve.function.SolOverrideResolver

class MissingVirtualInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionDefinition(o: SolFunctionDefinition) = check(o, "Function", "function")
      override fun visitModifierDefinition(o: SolModifierDefinition) = check(o, "Modifier", "modifier")

      private fun check(member: SolNamedElement, kind: String, kindLower: String) {
        val anchor = nameElement(member) ?: return
        // override site: every directly overridden declaration must be virtual
        for (decl in SolOverrideResolver.directlyOverridden(member)) {
          if (needsVirtual(decl)) {
            val label = SolOverrideResolver.label(decl)
            val fixes = if (AddVirtualFix.canEdit(decl)) arrayOf<LocalQuickFix>(AddVirtualFix(decl, label)) else emptyArray()
            holder.registerProblem(
              anchor,
              "Overridden $kindLower '$label' is not marked virtual",
              ProblemHighlightType.GENERIC_ERROR,
              *fixes
            )
          }
        }
        // base site: a declaration that is overridden somewhere must be virtual
        if (SolOverrideResolver.participates(member) && needsVirtual(member) &&
          SolOverrideResolver.overridesInDerived(member).isNotEmpty()
        ) {
          holder.registerProblem(
            anchor,
            "$kind is overridden but not marked virtual",
            ProblemHighlightType.GENERIC_ERROR,
            AddVirtualFix(member)
          )
        }
      }

      private fun needsVirtual(decl: SolNamedElement) =
        !SolOverrideResolver.isVirtual(decl) && SolOverrideResolver.hasBody(decl)
    }
  }
}
