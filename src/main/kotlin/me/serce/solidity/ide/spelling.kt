package me.serce.solidity.ide

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityTokenTypes

class SolSpellcheckingStrategy : SpellcheckingStrategy() {
  override fun isMyContext(element: PsiElement) = SolidityLanguage.`is`(element.language)

  override fun getTokenizer(element: PsiElement?): Tokenizer<*> = when {
    element?.node?.elementType == SolidityTokenTypes.STRINGLITERAL -> StringExpressionTokenizer
    else -> super.getTokenizer(element)
  }
}

/**
 * @see com.intellij.spellchecker.LiteralExpressionTokenizer
 */
object StringExpressionTokenizer : EscapeSequenceTokenizer<LeafPsiElement>() {
  override fun tokenize(element: LeafPsiElement, consumer: TokenConsumer) {
    val text = element.text

    if (!text.contains("\\")) {
      consumer.consumeToken(element, PlainTextSplitter.getInstance())
    } else {
      processTextWithEscapeSequences(element, text, consumer)
    }
  }

  private fun processTextWithEscapeSequences(element: LeafPsiElement, text: String, consumer: TokenConsumer) {
    val unescapedText = StringBuilder()
    val offsets = IntArray(text.length + 1)
    CodeInsightUtilCore.parseStringCharacters(text, unescapedText, offsets)
    EscapeSequenceTokenizer.processTextWithOffsets(element, consumer, unescapedText, offsets, 1)
  }
}





