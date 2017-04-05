package me.serce.solidity.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.stubs.SolidityFileStub

fun PsiElement.rangeRelativeTo(ancestor: PsiElement): TextRange {
  check(ancestor.textRange.contains(textRange))
  return textRange.shiftRight(-ancestor.textRange.startOffset)
}

inline fun <reified T : PsiElement> PsiElement.childOfType(strict: Boolean = true): T? =
  PsiTreeUtil.findChildOfType(this, T::class.java, strict)

val PsiElement.parentRelativeRange: TextRange
  get() = rangeRelativeTo(parent)

val PsiElement.ancestors: Sequence<PsiElement> get() = generateSequence(this) { it.parent }

val PsiElement.elementType: IElementType
  get() = if (this is SolidityFile) SolidityFileStub.Type else PsiUtilCore.getElementType(this)
