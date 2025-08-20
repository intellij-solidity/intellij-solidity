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

val KEYWORD_ON_CONTRACT = arrayOf("layout ")

val KEYWORD_FUNCTION = arrayOf(
  "this",
  "return",
  "while",
  "assembly",
  "assert",
  "require",
  "revert",
  "super",
  "if",
  "else",
  "delete",
  "payable",
  "new",
  "do",
  "continue",
  "try",
  "catch",
  "emit"
)

val KEYWORD_ON_FUNCTION = arrayOf("external ", "internal ", "public ", "private ", "virtual ", "override", "returns")

val KEYWORD_STATE_MUTABILITY = arrayOf("pure ", "view ", "payable ")

val KEYWORD_DATA_LOCATION = arrayOf(
  "memory", "storage", "calldata"
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
        ) + KEYWORD_TYPE + KEYWORD_ROOT_AND_BODY + KEYWORD_CONTRACT_BODY + KEYWORD_ON_CONTRACT)
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
      insideFunction(),
      SolKeywordCompletionProvider(*(arrayOf(*KEYWORD_FUNCTION + KEYWORD_ON_FUNCTION + KEYWORD_STATE_MUTABILITY)))
    )

    extend(
      CompletionType.BASIC, inFunctionParameterDef(), SolKeywordCompletionProvider(*(arrayOf(*KEYWORD_DATA_LOCATION)))
    )
    extend(
      CompletionType.BASIC, inImportDeclaration(), SolKeywordCompletionProvider("from ")
    )
    extend(
      CompletionType.BASIC,
      inStateVariableDeclaration(),
      SolKeywordCompletionProvider(
        "constant ",
        "internal ",
        "public ",
        "private ",
        "override ",
        "immutable ",
        "transient "
      )
    )

    extend(
      CompletionType.BASIC,
      inFunctionDeclaration(),
      SolKeywordCompletionProvider("external ", "virtual ", "override ", "payable ")
    )
    extend(
      CompletionType.BASIC,
      inConstructorDeclaration(),
      SolKeywordCompletionProvider("payable ", "internal ", "public ")
    )
  }
}
