package me.serce.solidity.ide.hints

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.parameterInfo.*
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.ref.SolFunctionCallReference

private const val INVALID_INDEX: Int = -2

class SolParameterInfoHandler : ParameterInfoHandler<PsiElement, SolArgumentsDescription> {
  var hintText: String = ""

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
    hintText = p.presentText
    context.setupUIComponentPresentation(
      hintText,
      range.startOffset,
      range.endOffset,
      !context.isUIComponentEnabled,
      false,
      false,
      context.defaultParameterColor)
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

class SolArgumentsDescription(val arguments: Array<String>) {
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
      if (ref is SolFunctionCallReference) {
        return ref.resolveFunctionCall()
          .mapNotNull {def ->
            val functionDef = def.element as SolFunctionDefinition
            val argumentDefList = (if (def.usingLibrary) {
              functionDef.parameterListList.firstOrNull()?.parameterDefList?.drop(1)
            } else {
              functionDef.parameterListList.firstOrNull()?.parameterDefList
            })
            argumentDefList?.let { list ->
              SolArgumentsDescription(list.map { "${it.typeName.text} ${it.identifier?.text ?: ""}" }.toTypedArray())
            }
          }
      } else {
        return emptyList()
      }
    }
  }
}
