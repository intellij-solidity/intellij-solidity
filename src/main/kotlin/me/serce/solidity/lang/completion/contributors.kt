package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.psi.SolBlock
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolPrimaryExpression
import me.serce.solidity.lang.psi.SolStatement

/**
 * Special Variables and Functions
 *
 * http://solidity.readthedocs.io/en/develop/units-and-global-variables.html#special-variables-and-functions
 */
class SolContextCompletionContributor : CompletionContributor(), DumbAware {
  init {
    // beginning of a statement inside a block
    extend(CompletionType.BASIC, startStatementInsideBlock(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
          val position = parameters.originalPosition
          if (position != null) {
            SolCompleter.completeLiteral(position)
              .forEach(result::addElement)
            SolCompleter.completeTypeName(position)
              .forEach(result::addElement)
          }
        }
      })

    // new expression after '=' inside a block
    extend(CompletionType.BASIC, eqExpressionInsideBlock(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
          val position = parameters.originalPosition
          if (position != null) {
            SolCompleter.completeLiteral(position)
              .forEach(result::addElement)
          }
        }
      })

    extend(CompletionType.BASIC, emitStartStatement(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
          SolCompleter
            .completeEventName(parameters.position)
            .map { insertParenthesis(it, true) }
            .forEach(result::addElement)
        }
      }
    )
  }

  private fun startStatementInsideBlock() = psiElement<PsiElement>()
    .withParent(SolBlock::class.java)
    .afterLeaf(
      or(
        // by some reason afterSibling doesn't work, it skips the intellijRulezzz marker
        psiElement().withText(";"),
        psiElement().withText("{")
      )
    )

  private fun eqExpressionInsideBlock() = psiElement<PsiElement>()
    .withParent(SolBlock::class.java)
    .afterLeaf(
      psiElement().withText("=")
    )
}

fun baseTypes() = hashSetOf("bool", "uint", "int", "fixed", "ufixed", "address", "byte", "bytes", "string");

class SolBaseTypesCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, stateVarInsideContract(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
          baseTypes()
            .asSequence()
            .map { "$it " }
            .map(LookupElementBuilder::create)
            .map(result::addElement)
            .toList()
        }
      })
  }
}

private fun <E> or(vararg patterns: ElementPattern<E>) = StandardPatterns.or(*patterns)

fun statement() = psiElement<PsiElement>()
  .inside(SolStatement::class.java)

fun insideContract() = psiElement<PsiElement>()
  .inside(SolContractDefinition::class.java)

private inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
  return psiElement(I::class.java)
}

fun LookupElementBuilder.keywordPrioritised(): LookupElement = PrioritizedLookupElement.withPriority(this, KEYWORD_PRIORITY)

fun insertParenthesis(elem: LookupElementBuilder, finish: Boolean): LookupElementBuilder =
        elem.withInsertHandler { ctx, _ ->
            ctx.document.insertString(ctx.selectionEndOffset, if (finish) "();" else "()")
            EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
        }
