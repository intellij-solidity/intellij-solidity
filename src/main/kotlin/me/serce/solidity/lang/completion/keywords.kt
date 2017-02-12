package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import me.serce.solidity.lang.psi.SolidityBlock
import me.serce.solidity.lang.psi.SolidityPrimaryExpression

  val KEYWORD_PRIORITY = 10.0

  class SolidityKeywordCompletionProvider(
private vararg val keywords: String
) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    keywords
      .map { LookupElementBuilder.create(it) }
      .forEach { result.addElement(PrioritizedLookupElement.withPriority(it, KEYWORD_PRIORITY)) }
  }
}


class SolidityKeywordCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, rootDeclaration(),
      SolidityKeywordCompletionProvider("pragma", "import", "contract", "library"))
    extend(CompletionType.BASIC, block(),
      SolidityKeywordCompletionProvider("sha3"))
  }

  private fun rootDeclaration() = psiElement<PsiElement>()
    .withParent(SolidityPrimaryExpression::class.java)

  private fun block() = psiElement<PsiElement>()
    .withParent(SolidityBlock::class.java)
}

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
  return psiElement(I::class.java)
}

