package me.serce.solidity.ide.hints

import com.intellij.lang.parameterInfo.CreateParameterInfoContext
import com.intellij.lang.parameterInfo.ParameterInfoHandler
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class AbstractParameterInfoHandler<O : PsiElement, T : Any> : ParameterInfoHandler<O, T> {

  abstract fun findTargetElement(file: PsiFile, offset: Int): O?

  abstract fun calculateParameterInfo(element: O): Array<T>?

  final override fun findElementForParameterInfo(context: CreateParameterInfoContext): O? {
    val element = findTargetElement(context.file, context.offset) ?: return null
    context.itemsToShow = calculateParameterInfo(element) ?: return null
    return element
  }

  final override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): O? {
    return findTargetElement(context.file, context.offset)
  }

  override fun showParameterInfo(element: O, context: CreateParameterInfoContext) {
    context.showHint(element, element.startOffset, this)
  }
}

val PsiElement.startOffset: Int
  get() = textRange.startOffset
