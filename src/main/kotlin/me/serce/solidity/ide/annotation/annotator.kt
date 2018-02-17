package me.serce.solidity.ide.annotation

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import me.serce.solidity.ide.actions.ImportFileFix
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.lang.psi.*

class SolidityAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is SolElement) {
      when {
        element is SolUserDefinedTypeName && element.reference != null && element.reference?.resolve() == null -> {
          holder.createWarningAnnotation(element, null).let {
            it.highlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            it.registerFix(ImportFileFix(element))
          }
        }
        else -> {
          val highlight = highlight(element)
          if (highlight != null) {
            val (partToHighlight, color) = highlight
            holder.createInfoAnnotation(partToHighlight, null).textAttributes = color.textAttributesKey
          }
        }
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
