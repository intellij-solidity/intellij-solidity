package me.serce.solidity.ide.hints

import com.intellij.lang.parameterInfo.ParameterInfoUIContext
import com.intellij.lang.parameterInfo.ParameterInfoUtils
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.endOffset
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import me.serce.solidity.lang.psi.SolCallable
import me.serce.solidity.lang.psi.SolFunctionCallArguments
import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference
import me.serce.solidity.lang.types.findParentOrNull

class SolParameterInfoHandler : AbstractParameterInfoHandler<PsiElement, SolArgumentsDescription>() {
  override fun findTargetElement(file: PsiFile, offset: Int): PsiElement? {
    val element = file.findElementAt(offset) ?: return null
    val functionCallParent = element.findParentOrNull<SolFunctionCallExpression>()
    if (functionCallParent != null) {
      return functionCallParent
    } else {
      var currentElement = if (element.prevSibling == null) {
        element.parent
      } else {
        element
      }
      var lParenFound = false
      while (currentElement.prevSibling != null) {
        currentElement = currentElement.prevSibling
        if (!lParenFound && currentElement.text == LPAREN.toString()) {
          lParenFound = true
        } else if (lParenFound && currentElement.text != null && currentElement.text.isNotBlank()) {
          return currentElement
        }
      }
    }
    return null
  }

  override fun calculateParameterInfo(element: PsiElement): Array<SolArgumentsDescription>? {
    val callElement : PsiElement=
      element.childrenOfType<SolFunctionCallExpression>().firstOrNull() ?: element
    val descriptions = SolArgumentsDescription.findDescriptions(callElement)
    return descriptions.takeIf { it.isNotEmpty() }?.toTypedArray()

  }

  override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
    if (context.parameterOwner != parameterOwner) {
      context.removeHint()
      return
    }
    val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
      -1
    } else if (parameterOwner is SolFunctionCallExpression) {
      ParameterInfoUtils.getCurrentParameterIndex(
        parameterOwner.functionCallArguments.node,
        context.offset,
        COMMA
      )
    } else {
      var indexArgument = -1
      var currentOffset = parameterOwner.startOffset
      var currentElement = parameterOwner
      while (currentOffset < context.offset) {
        if (indexArgument == -1 && currentElement.text == LPAREN.toString()) {
          indexArgument = 0
        } else if (currentElement.text == COMMA.toString()) {
          indexArgument++
        }
        currentOffset = currentElement.endOffset
        currentElement = currentElement.nextSibling
      }
      indexArgument
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
  callArguments: List<PsiElement>,
  val arguments: Array<String>
) {

    val valid = if (callArguments.isNotEmpty() && callArguments.first().parent is SolFunctionCallArguments) {
        callable.canBeApplied(callArguments.first().parent as SolFunctionCallArguments)
    } else {
        false
    }
  val presentText = if (arguments.isEmpty()) "<no parameters>" else arguments.joinToString(", ")

  fun getArgumentRange(index: Int): TextRange {
    if (index < 0 || index >= arguments.size) {
      return TextRange.EMPTY_RANGE
    }
    val start = arguments.take(index).sumOf { it.length + 2 }
    return TextRange(start, start + arguments[index].length)
  }

  companion object {
    fun findDescriptions(call: PsiElement): List<SolArgumentsDescription> {
      //some elements are not functionCallExpression yet mostly due to a missing ';' at the end

      val ref = call.reference
      return if (ref is SolFunctionCallReference && call is SolFunctionCallExpression) {
        ref.resolveFunctionCall().map { def ->
          val parameters =
            def.parseParameters().map { "${it.second}${it.first?.let { name -> " $name" } ?: ""}" }.toTypedArray()
          SolArgumentsDescription(def, call.functionCallArguments.expressionList, parameters)
        }
      } else {
        val currentArguments: List<PsiElement> = getArgumentsFromPsiElement(call)
        SolResolver.lexicalDeclarations(call).filter { it.name == call.text }.filterIsInstance<SolCallable>()
          .map { def ->
            val parameters =
              def.parseParameters().map { "${it.second}${it.first?.let { name -> " $name" } ?: ""}" }.toTypedArray()
            SolArgumentsDescription(def, currentArguments, parameters)
          }.toList()
      }
    }

    fun getArgumentsFromPsiElement(element: PsiElement): List<PsiElement> {
      val arguments = mutableListOf<PsiElement>()
      var currentElement: PsiElement? = element.nextSibling
      while (currentElement != null && currentElement.text != RPAREN.toString()) {
        if (currentElement.text != LPAREN.toString() && currentElement.text != COMMA.toString() && currentElement.text.isNotBlank()) {
          arguments.add(currentElement)
        }
        currentElement = currentElement.nextSibling
      }
      return arguments
    }
  }
}
