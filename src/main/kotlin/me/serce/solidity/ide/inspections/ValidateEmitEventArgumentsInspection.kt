package me.serce.solidity.ide.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import me.serce.solidity.ide.hints.NO_VALIDATION_TAG
import me.serce.solidity.ide.hints.comments
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolUnknown
import me.serce.solidity.lang.types.type

class ValidateEmitEventArgumentsInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitEmitStatement(o: SolEmitStatement) {
        fun PsiElement.getDefs(): List<SolEventDefinition>? {
          return (o.parent?.children?.firstOrNull() as? SolEmitStatement)?.let {
            (it.lastChild as? SolFunctionCallExpression)?.let {
              SolResolver.resolveVarLiteralReference(it)
            }
          }?.filterIsInstance<SolEventDefinition>()
        }

        fun PsiElement.getParameters(): List<SolExpression>? {
          return (o.parent?.children?.firstOrNull() as? SolEmitStatement)?.let {
            (it.lastChild as? SolFunctionCallExpression)?.functionCallArguments?.expressionList
          }
        }

        val args = o.getParameters()

        if (args?.size == 0) {
          holder.registerProblem(o, "No arguments")

          return
        }

        o.getDefs()?.let {
          if (it.isEmpty()) {
            holder.registerProblem(o.parent?.firstChild?.lastChild?.firstChild
              ?: o, "Can't find the event in that contract")

            return
          }

          val f = it.filterNot { it.comments().any { it.elementType == SolidityTokenTypes.NAT_SPEC_TAG && it.text == NO_VALIDATION_TAG } }
          if (f.isNotEmpty()) {
            var wrongNumberOfArgs = ""
            var wrongTypes = ""
            var wrongElement = (o.parent?.firstChild?.lastChild?.firstChild ?: o) as SolElement
            if (f.none { ref ->
                val expParameters = ref.parseParameters()
                val expArgs = expParameters.size
                val actArgs = args?.size ?: 0
                if (actArgs != expArgs) {
                  wrongNumberOfArgs = "Expected $expArgs argument${if (expArgs > 1) "s" else ""}, but got $actArgs"
                  false
                } else {
                  args!!.withIndex().all { argtype ->
                    expParameters.getOrNull(argtype.index)?.let {
                      val expType = it.second
                      val actType = argtype.value.type
                      expType == SolUnknown || actType == SolUnknown || expType.isAssignableFrom(actType).also {
                        if (!it) {
                          wrongTypes = "Argument of type '$actType' is not assignable to parameter of type '${expType}'"
                          wrongElement = argtype.value
                        }
                      }
                    } == true
                  }
                }
              }) {
              holder.registerProblem(
                o, wrongTypes.takeIf { it.isNotEmpty() }
                ?: wrongNumberOfArgs)
            }
          }
        }
      }
    }
  }
}
