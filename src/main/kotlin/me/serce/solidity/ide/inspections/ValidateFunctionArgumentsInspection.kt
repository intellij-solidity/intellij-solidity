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
import me.serce.solidity.lang.types.SolInternalTypeFactory
import me.serce.solidity.lang.types.SolUnknown
import me.serce.solidity.lang.types.getSolType
import me.serce.solidity.lang.types.type

class ValidateFunctionArgumentsInspection : LocalInspectionTool() {
  override fun getDisplayName(): String = ""

  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
    return object : SolVisitor() {
      override fun visitFunctionCallArguments(element: SolFunctionCallArguments) {
        val args = element.expressionList
        fun PsiElement.getDefs(): List<SolFunctionDefElement>? {
          return ((parent.parent?.children?.firstOrNull() as? SolMemberAccessExpression)?.let {
            SolResolver.resolveMemberAccess(it)
          } ?: (parent?.parent as? SolFunctionCallExpression)?.let {
            SolResolver.resolveVarLiteralReference(it)
          })?.filterIsInstance<SolFunctionDefElement>()
        }
        if (args.firstOrNull() !is SolMapExpression) {
          element.getDefs()?.let {
            val funDefs = it.filterNot { it.comments().any { it.elementType == SolidityTokenTypes.NAT_SPEC_TAG && it.text == NO_VALIDATION_TAG } }
            if (funDefs.isNotEmpty()) {
              var wrongNumberOfArgs = ""
              var wrongTypes = ""
              var wrongElement = element as SolElement
              if (funDefs.none { ref ->
                  var parameters = ref.parameters
                  val expArgs = parameters.size
                  val actArgs = args.size
                  val vararg = parameters.find { it.name == SolInternalTypeFactory.varargsId }
                  if (actArgs != expArgs && vararg == null) {
                    wrongNumberOfArgs = "Expected $expArgs argument${if (expArgs > 1) "s" else ""}, but got $actArgs"
                    false
                  } else {
                    if (vararg != null && actArgs > expArgs) {
                      parameters = parameters.toMutableList() + List(actArgs - expArgs) {vararg}
                    }
                    args.withIndex().all { argtype ->
                      parameters.getOrNull(argtype.index)?.let {
                        val expType = getSolType(it.typeName)
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
                  element, wrongTypes.takeIf { it.isNotEmpty() }
                  ?: wrongNumberOfArgs)
              }
            }
          }
        }
      }

    }
  }
}
