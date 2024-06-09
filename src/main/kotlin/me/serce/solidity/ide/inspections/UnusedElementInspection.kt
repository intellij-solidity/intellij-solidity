package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parents
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolConstructorOrFunctionDef
import me.serce.solidity.lang.resolve.SolResolver

class UnusedElementInspection : LocalInspectionTool() {

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitImportDirective(o: SolImportDirective) {
        val used = SolResolver.collectUsedElements(o)
        if (used.isEmpty()) {
          holder.registerProblem(o, "Unused import directive", ProblemHighlightType.LIKE_UNUSED_SYMBOL)
        }
      }

      override fun visitConstantVariableDeclaration(o: SolConstantVariableDeclaration) {
        o.identifier.checkForUsage(o, holder, "Constant '${o.name}' is never used")
      }

      override fun visitEnumDefinition(o: SolEnumDefinition) {
        o.identifier?.checkForUsage(o, holder, "Enum '${o.name}' is never used")
      }

      override fun visitFunctionDefinition(o: SolFunctionDefinition) {
        o.identifier?.takeIf { o.visibility != Visibility.EXTERNAL }?.checkForUsage(o, holder, "Function '${o.name}' is never used")
      }

      override fun visitStructDefinition(o: SolStructDefinition) {
        o.identifier?.checkForUsage(o, holder, "Struct '${o.name}' is never used")
      }

      override fun visitStateVariableDeclaration(o: SolStateVariableDeclaration) {
        o.identifier.checkForUsage(o, holder, "State variable '${o.name}' is never used")
      }

      override fun visitVariableDeclaration(o: SolVariableDeclaration) {
        o.identifier?.checkForUsage(o, holder, "Variable '${o.name}' is never used")
      }

      override fun visitParameterDef(o: SolParameterDef) {
        o.identifier?.takeIf { o.parents(false).filterIsInstance<SolConstructorOrFunctionDef>().firstOrNull()?.getBlock() != null }?.checkForUsage(o, holder, "Parameter '${o.name}' is never used")
      }

      override fun visitModifierDefinition(o: SolModifierDefinition) {
        o.identifier?.checkForUsage(o, holder, "Modifier '${o.name}' is never used")
      }
    }
  }
}

private fun PsiElement.checkForUsage(o: PsiElement, holder: ProblemsHolder, msg: String) {
  if (ReferencesSearch.search(o).findFirst() == null) {
    holder.registerProblem(this, msg, ProblemHighlightType.LIKE_UNUSED_SYMBOL)
  }
}
