package me.serce.solidity.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.SolCallable
import me.serce.solidity.lang.psi.SolFunctionCallArguments
import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.types.findParentOrNull

class SolParameterInfoHandler : AbstractParameterInfoHandler<SolFunctionCallExpression, SolArgumentsDescription>() {
  override fun findTargetElement(file: PsiFile, offset: Int): SolFunctionCallExpression? {
    return file.findElementAt(offset)?.findParentOrNull()
  }

  override fun calculateParameterInfo(element: SolFunctionCallExpression): Array<SolArgumentsDescription>? {
    val result = SolArgumentsDescription.findDescriptions(element)
    if (result.isEmpty()) return null
    return result.toTypedArray()
  }

  override fun updateParameterInfo(parameterOwner: SolFunctionCallExpression, context: UpdateParameterInfoContext) {
    if (context.parameterOwner != parameterOwner) {
      context.removeHint()
      return
    }
    val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
      -1
    } else {
      ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.functionCallArguments.node, context.offset, SolidityTokenTypes.COMMA)
    }
    context.setCurrentParameter(currentParameterIndex)
  }

  override fun updateUI(p: SolArgumentsDescription, context: ParameterInfoUIContext) {
    val range = p.getArgumentRange(context.currentParameterIndex)
    context.setupUIComponentPresentation(
      p.presentText,
      range.startOffset,
      range.endOffset,
      !context.isUIComponentEnabled,
      false,
      false,
      if (p.valid) context.defaultParameterColor.brighter() else context.defaultParameterColor)
  }
}

class SolArgumentsDescription(
  callable: SolCallable,
  callArguments: SolFunctionCallArguments,
  val arguments: Array<String>
) {

  val valid = callable.canBeApplied(callArguments)
  val presentText = if (arguments.isEmpty()) "<no arguments>" else arguments.joinToString(", ")

  fun getArgumentRange(index: Int): TextRange {
    if (index < 0 || index >= arguments.size) {
      return TextRange.EMPTY_RANGE
    }
    val start = arguments.take(index).sumOf { it.length + 2 }
    return TextRange(start, start + arguments[index].length)
  }

  companion object {
    fun findDescriptions(call: SolFunctionCallExpression): List<SolArgumentsDescription> {
      val ref = call.reference
      return if (ref is SolFunctionCallReference) {
        ref.resolveFunctionCall()
          .map { def ->
            val parameters = def.parseParameters()
              .map { "${it.second}${it.first?.let { name -> " $name" } ?: ""}" }
              .toTypedArray()
            SolArgumentsDescription(def, call.functionCallArguments, parameters)
          }
      } else {
        emptyList()
      }
    }
  }
}
