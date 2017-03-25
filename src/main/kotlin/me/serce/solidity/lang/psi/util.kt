package me.serce.solidity.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

fun PsiElement.rangeRelativeTo(ancestor: PsiElement): TextRange {
  check(ancestor.textRange.contains(textRange))
  return textRange.shiftRight(-ancestor.textRange.startOffset)
}

val PsiElement.parentRelativeRange: TextRange
  get() = rangeRelativeTo(parent)
