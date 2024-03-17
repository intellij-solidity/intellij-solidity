package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.SolImportDirective
import me.serce.solidity.lang.psi.SolVisitor
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
    }
  }
}
