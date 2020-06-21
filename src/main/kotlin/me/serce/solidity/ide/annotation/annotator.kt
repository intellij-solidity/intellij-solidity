package me.serce.solidity.ide.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.lang.psi.SolElement
import me.serce.solidity.lang.psi.SolElementaryTypeName
import me.serce.solidity.lang.psi.SolNumberType
import me.serce.solidity.lang.psi.SolUserDefinedTypeName

class SolidityAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is SolElement) {
      val highlight = highlight(element)
      if (highlight != null) {
        val (partToHighlight, color) = highlight
        holder.newAnnotation(HighlightSeverity.INFORMATION, "")
          .range(partToHighlight)
          .textAttributes(color.textAttributesKey)
          .create()
      }
    }
  }

  private fun highlight(element: SolElement): Pair<PsiElement, SolColor>? {
    return when (element) {
      is SolNumberType -> element to SolColor.KEYWORD
      is SolElementaryTypeName -> element to SolColor.KEYWORD
      is SolUserDefinedTypeName -> element to SolColor.CONTRACT_REFERENCE
      else -> null
    }
  }
}
