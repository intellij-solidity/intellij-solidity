package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.ide.inspections.fixes.ImportFileFix
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import me.serce.solidity.lang.psi.SolVisitor

class ResolveNameInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitUserDefinedTypeName(element: SolUserDefinedTypeName) {
        if (element.reference != null && element.reference?.resolve() == null) {
          holder.registerProblem(element, "Import File", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, ImportFileFix(element))
        }
      }
    }
  }
}
