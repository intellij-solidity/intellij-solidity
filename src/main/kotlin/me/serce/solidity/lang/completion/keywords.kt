package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ProcessingContext

const val KEYWORD_PRIORITY = 10.0

val KEYWORD_TYPE = arrayOf(
  "address ",
  "string ",
  "fixed",
  "ufixed",
  "uint8 ",
  "uint16 ",
  "uint32 ",
  "uint64 ",
  "uint128 ",
  "uint256 ",
  "int8 ",
  "int16 ",
  "int32 ",
  "int64 ",
  "int128 ",
  "int256 ",
  "bytes ",
  "bytes4 ",
  "bytes8 ",
  "bytes16 ",
  "bytes20 ",
  "bytes32 ",
  "byte ",
  "bool "
)

val KEYWORD_CONTRACT_BODY = arrayOf(
  "constructor", "function ", "modifier ", "fallback", "receive", "mapping", "this"
)

val KEYWORD_ROOT_AND_BODY = arrayOf(
  "enum ", "struct ", "event ", "error ", "using ", "type "
)

class SolKeywordCompletionProvider(private vararg val keywords: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet
  ) {
    keywords.map { LookupElementBuilder.create(it) }.forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolKeywordCompletionContributor : CompletionContributor(), DumbAware {
  init {

    extend(
      CompletionType.BASIC, rootDeclaration(), SolKeywordCompletionProvider(
        *(arrayOf(
          "pragma ",
          "import ",
          "contract ",
          "library ",
          "interface ",
          "abstract ",
        ) + KEYWORD_TYPE + KEYWORD_ROOT_AND_BODY)

      )
    )
    extend(CompletionType.BASIC, rootDeclaration(), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
      ) {
        val pragmaBuilder = LookupElementBuilder
          .create("pragma solidity")
          .bold()
          .withTailText(" ^...")
          .withInsertHandler { ctx, _ ->
            ctx.document.insertString(ctx.selectionEndOffset, " ^0.8.0;")
            EditorModificationUtil.moveCaretRelatively(ctx.editor, 9)
          }
        result.addElement(PrioritizedLookupElement.withPriority(pragmaBuilder, KEYWORD_PRIORITY - 5))
      }
    })

    extend(
      CompletionType.BASIC,
      insideContract().andNot(inMemberAccess()),
      SolKeywordCompletionProvider(*(arrayOf(*KEYWORD_TYPE, *KEYWORD_ROOT_AND_BODY, *KEYWORD_CONTRACT_BODY)))
    )
  }
}
