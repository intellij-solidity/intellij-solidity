package me.serce.solidity.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.canBeApplied
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference

private const val INVALID_INDEX: Int = -2

class SolParameterInfoHandler : ParameterInfoHandler<PsiElement, SolArgumentsDescription> {
  override fun findElementForParameterInfo(context: CreateParameterInfoContext): PsiElement? {
    val contextElement = context.file?.findElementAt(context.editor.caretModel.offset) ?: return null
    return findElementForParameterInfo(contextElement)
  }

  override fun showParameterInfo(element: PsiElement, context: CreateParameterInfoContext) {
    if (element !is SolFunctionCallExpression) {
      return
    }
    val descriptions = SolArgumentsDescription.findDescriptions(element)
    context.itemsToShow = descriptions.toTypedArray()
    context.showHint(element, element.textRange.startOffset, this)
  }

  override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext) =
    context.file.findElementAt(context.editor.caretModel.offset)

  override fun updateUI(p: SolArgumentsDescription?, context: ParameterInfoUIContext) {
    if (p == null) {
      context.isUIComponentEnabled = false
      return
    }
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

  override fun updateParameterInfo(parameterOwner: PsiElement, context: UpdateParameterInfoContext) {
    val argIndex = findArgumentIndex(parameterOwner)
    if (argIndex == INVALID_INDEX) {
      context.removeHint()
      return
    }
    context.setCurrentParameter(argIndex)
    when {
      context.parameterOwner == null -> {
        context.parameterOwner = parameterOwner
      }
      context.parameterOwner != findElementForParameterInfo(parameterOwner) -> {
        context.removeHint()
        return
      }
    }
    context.objectsToView.indices.map { context.setUIComponentEnabled(it, true) }
  }

  override fun getParametersForDocumentation(p: SolArgumentsDescription, context: ParameterInfoContext?) =
    p.arguments

  override fun getParametersForLookup(item: LookupElement, context: ParameterInfoContext?): Array<out Any>? {
    val el = item.`object` as? PsiElement ?: return null
    val p = el.parent?.parent
    return if (p is SolFunctionCallExpression) arrayOf(p) else emptyArray()
  }

  private fun findArgumentIndex(place: PsiElement): Int {
    val callArgs = place.parentOfType<SolFunctionCallExpression>()
    @Suppress("FoldInitializerAndIfToElvis")
    if (callArgs == null) {
      return INVALID_INDEX
    }
    val descriptions = SolArgumentsDescription.findDescriptions(callArgs)
    if (descriptions.isEmpty()) {
      return INVALID_INDEX
    }
    var index = -1
    val arguments = callArgs.functionCallArguments
    if (arguments != null) {
      index += generateSequence(arguments.firstChild) { c -> c.nextSibling }
        .filter { it.text == "," }
        .count { it.textRange.startOffset < place.textRange.startOffset } + 1
    }
    return index
  }

  private fun findElementForParameterInfo(contextElement: PsiElement) =
    contextElement.parentOfType<SolFunctionCallExpression>()

  override fun getParameterCloseChars() = ",)"

  override fun tracksParameterIndex() = true

  override fun couldShowInLookup() = true
}

class SolArgumentsDescription(
  callable: ResolvedCallable,
  callArguments: SolFunctionCallArguments,
  val arguments: Array<String>
) {

  val valid = callable.canBeApplied(callArguments)
  val presentText = if (arguments.isEmpty()) "<no arguments>" else arguments.joinToString(", ")

  fun getArgumentRange(index: Int): TextRange {
    if (index < 0 || index >= arguments.size) {
      return TextRange.EMPTY_RANGE
    }
    val start = arguments.take(index).sumBy { it.length + 2 }
    return TextRange(start, start + arguments[index].length)
  }

  companion object {
    fun findDescriptions(element: SolFunctionCallExpression): List<SolArgumentsDescription> {
      val ref = element.reference
      return if (ref is SolFunctionCallReference) {
        ref.resolveFunctionCall()
          .map { def ->
            val parameters = def.parseParameters()
            SolArgumentsDescription(def, element.functionCallArguments, parameters.map { "${it.second}${it.first?.let { name -> " $name" } ?: ""}" }.toTypedArray())
          }
      } else {
        emptyList()
      }
    }
  }
}
