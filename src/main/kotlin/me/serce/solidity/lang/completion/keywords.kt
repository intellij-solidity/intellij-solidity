package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ProcessingContext

const val KEYWORD_PRIORITY = 10.0

class SolKeywordCompletionProvider(private vararg val keywords: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    keywords
      .map { LookupElementBuilder.create(it) }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolKeywordCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(
      CompletionType.BASIC, rootDeclaration(),
      SolKeywordCompletionProvider("pragma ", "import ", "contract ", "library "))
    extend(CompletionType.BASIC, rootDeclaration(), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val pragmaBuilder = LookupElementBuilder
          .create("pragma solidity")
          .bold()
          .withTailText(" ^...")
          .withInsertHandler { ctx, _ ->
            ctx.document.insertString(ctx.selectionEndOffset, " ^0.5.4;")
            EditorModificationUtil.moveCaretRelatively(ctx.editor, 9)
          }
        result.addElement(PrioritizedLookupElement.withPriority(pragmaBuilder, KEYWORD_PRIORITY - 5))
      }
    })

    extend(CompletionType.BASIC, insideContract().andNot(inMemberAccess()), SolKeywordCompletionProvider("this"))
  }
}
