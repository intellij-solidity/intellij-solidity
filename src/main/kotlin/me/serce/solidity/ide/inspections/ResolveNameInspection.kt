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
        if (element.reference != null) {
          // resolve return either 1 reference or null, and because our resolve is not perfect we can return a number
          // of references, so instead of showing false positives we can use multiresolve
          val results = element.reference?.multiResolve(false)
          if (results == null || results.isEmpty()) {
            holder.registerProblem(element, "Import File", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, ImportFileFix(element))
          }
        }
      }
    }
  }
}
