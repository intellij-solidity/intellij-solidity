package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolVisitor
import me.serce.solidity.lang.psi.impl.LinearizationImpossibleException
import me.serce.solidity.lang.types.SolContract

class LinearizationImpossibleInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitContractDefinition(o: SolContractDefinition) {
        try {
          SolContract(o).linearize()
        } catch (e: LinearizationImpossibleException) {
          holder.registerProblem(o.identifier!!, "Linearization of inheritance graph impossible: ${e.message}")
        }
      }
    }
  }
}

