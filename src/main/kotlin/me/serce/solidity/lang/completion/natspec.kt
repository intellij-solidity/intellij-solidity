package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import me.serce.solidity.lang.core.SolidityTokenTypes

// https://docs.soliditylang.org/en/develop/natspec-format.html#tags
private val NATSPEC_TAGS = arrayListOf(
  "title" to "A title that should describe the contract/interface",
  "author" to "The name of the author",
  "notice" to "Explain to an end user what this does",
  "dev" to "Explain to a developer any extra details",
  "param" to "Documents a parameter just like in Doxygen",
  "return" to "Documents the return variables of a contract’s function",
  "inheritdoc" to "Copies all missing tags from the base function"
)

class NatSpecCompletionProvider(val prefix: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    NATSPEC_TAGS
      .map { (tag, descr) -> LookupElementBuilder.create(prefix + tag).withTailText(" $descr") }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolNatSpecCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(
      CompletionType.BASIC,
      psiElement(SolidityTokenTypes.NAT_SPEC_TAG),
      NatSpecCompletionProvider("@"))
    extend(
      CompletionType.BASIC,
      psiElement(SolidityTokenTypes.COMMENT),
      NatSpecCompletionProvider(""))
  }
}
