package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolidityContractPart
import me.serce.solidity.lang.psi.SolidityPrimaryExpression
import me.serce.solidity.lang.psi.SolidityStatement

/**
 * Special Variables and Functions
 *
 * http://solidity.readthedocs.io/en/develop/units-and-global-variables.html#special-variables-and-functions
 */

val KEYWORD_PRIORITY = 10.0

class SolidityKeywordCompletionProvider(vararg val keywords: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    keywords
      .map { LookupElementBuilder.create(it) }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SoliditySimpleFunctionCompletionProvider(vararg val functions: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    functions
      .map {
        LookupElementBuilder.create(it).withTailText("()")
          .withInsertHandler { context, lookupElement ->
            context.document.insertString(context.selectionEndOffset, "()")
            EditorModificationUtil.moveCaretRelatively(context.editor, 1)
          }
      }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolidityKeywordCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, rootDeclaration(),
      SolidityKeywordCompletionProvider("pragma ", "import ", "contract ", "library "))
    extend(CompletionType.BASIC, rootDeclaration(), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        val pragmaBuilder = LookupElementBuilder
          .create("pragma solidity")
          .bold()
          .withTailText(" ^...")
          .withInsertHandler { context, lookupElement ->
            context.document.insertString(context.selectionEndOffset, " ^0.4.4;")
            EditorModificationUtil.moveCaretRelatively(context.editor, 9)
          }
        result.addElement(PrioritizedLookupElement.withPriority(pragmaBuilder, KEYWORD_PRIORITY - 5))
      }
    })

    extend(CompletionType.BASIC, statement(),
      SoliditySimpleFunctionCompletionProvider("assert", "addmod", "mulmod", "keccak256", "sha3", "sha256", "ripemd160",
        "ecrecover", "revert"))

    extend(CompletionType.BASIC, insideContract(),
      SolidityKeywordCompletionProvider("this"))
    extend(CompletionType.BASIC, insideContract(),
      SoliditySimpleFunctionCompletionProvider("selfdestruct"))
  }

  private fun rootDeclaration() = psiElement<PsiElement>()
    .withParents(SolidityPrimaryExpression::class.java, SolidityFile::class.java)

  private fun statement() = psiElement<PsiElement>()
    .inside(SolidityStatement::class.java)

  private fun insideContract() = psiElement<PsiElement>()
    .inside(SolidityContractPart::class.java)

}

inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
  return psiElement(I::class.java)
}

private fun LookupElementBuilder.keywordPrioritised() = PrioritizedLookupElement.withPriority(this, KEYWORD_PRIORITY)

