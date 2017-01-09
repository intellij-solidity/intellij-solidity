package me.serce.solidity.ide

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.SolidityElement
import me.serce.solidity.lang.psi.SolidityElementaryTypeName
import me.serce.solidity.lang.psi.SolidityNumberType
import me.serce.solidity.lang.psi.SolidityUserDefinedTypeName
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

class SolidityAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is SolidityElement) {
      val highlight = highlight(element)
      if (highlight != null) {
        val (partToHighlight, color) = highlight
        holder.createInfoAnnotation(partToHighlight, null).textAttributes = color
      }
    }
  }

  private fun highlight(element: SolidityElement): Pair<PsiElement, TextAttributesKey>? {
    return when (element) {
      is SolidityNumberType -> element to Defaults.KEYWORD
      is SolidityElementaryTypeName -> element to Defaults.KEYWORD

      is SolidityUserDefinedTypeName -> element to Defaults.CLASS_REFERENCE

      else -> null
    }
  }
}
