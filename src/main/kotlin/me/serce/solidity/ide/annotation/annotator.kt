package me.serce.solidity.ide.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.SolElementaryTypeName
import me.serce.solidity.lang.psi.SolNumberType
import me.serce.solidity.lang.psi.SolUserDefinedTypeName
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

class SolidityAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is SolElement) {
      val highlight = highlight(element)
      if (highlight != null) {
        val (partToHighlight, color) = highlight
        holder.createInfoAnnotation(partToHighlight, null).textAttributes = color
      }
    }
  }

  private fun highlight(element: SolElement): Pair<PsiElement, TextAttributesKey>? {
    return when (element) {
      is SolNumberType -> element to Defaults.KEYWORD
      is SolElementaryTypeName -> element to Defaults.KEYWORD

      is SolUserDefinedTypeName -> element to Defaults.CLASS_REFERENCE

      else -> null
    }
  }
}
