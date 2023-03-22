package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.*
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.ProcessingContext
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.ide.hints.SolArgumentsDescription
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*

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
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
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
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
          val position = parameters.originalPosition
          if (position != null) {
            SolCompleter.completeLiteral(position)
              .forEach(result::addElement)
          }
        }
      })

    extend(CompletionType.BASIC, emitStartStatement(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
          SolCompleter
            .completeEventName(parameters.position)
            .map { it.insertParenthesis(true) }
            .forEach(result::addElement)
        }
      }
    )

    extend(CompletionType.BASIC, revertStartStatement(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
          SolCompleter
            .completeErrorName(parameters.position)
            .map { it.insertParenthesis(true) }
            .forEach(result::addElement)
        }
      }
    )

    extend(CompletionType.BASIC, mappingExpression(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
          val descriptions = SolArgumentsDescription.findDescriptions(parameters.originalPosition?.parentOfType() ?: return)
          val defined = (parameters.originalPosition?.parentOfType<SolMapExpression>())?.mapExpressionClauseList?.map { it.identifier.text }?.toSet() ?: emptySet()
          val needComma = parameters.originalPosition?.elementType != SolidityTokenTypes.RBRACE
          val elements = (descriptions.flatMap { it.arguments.map { it.split(" ").last() }.toList() }.toSet() - defined)
            .map {
              LookupElementBuilder.create(it).withIcon(SolidityIcons.STATE_VAR).withInsertHandler { context, item ->
                val originalPosition = parameters.originalPosition
                val parent = originalPosition?.parent
                if (parent !is SolMapExpressionClause) {
                  val insert = " : ${if (needComma) "," else ""}"
                  context.document.insertString(context.selectionEndOffset, insert)
                  context.editor.caretModel.currentCaret.moveToOffset(context.selectionEndOffset - 1)
                }
              }
            }
          result.addAllElements(elements)
        }
      }
    )
  }

  private fun mappingExpression() = ObjectPattern.Capture(object : InitialPatternCondition<SolMapExpression>(SolMapExpression::class.java) {
    override fun accepts(o: Any?, context: ProcessingContext?): Boolean {
      val element = o as? LeafPsiElement ?: return false
      if (element.elementType != SolidityTokenTypes.IDENTIFIER) return false
      return element.parent is SolMapExpression || element.parent is SolMapExpressionClause
    }
  })

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

fun baseTypes() = hashSetOf("bool", "uint", "int", "fixed", "ufixed", "address", "byte", "bytes", "string")

class SolBaseTypesCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, stateVarInsideContract(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
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

fun statement(): PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
  .inside(SolStatement::class.java)

fun insideContract(): PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
  .inside(SolContractDefinition::class.java)

fun inMemberAccess(): PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
  .withParent(SolMemberAccessExpression::class.java)

private inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
  return psiElement(I::class.java)
}

fun LookupElementBuilder.keywordPrioritised(): LookupElement = PrioritizedLookupElement.withPriority(this, KEYWORD_PRIORITY)

fun LookupElementBuilder.insertParenthesis(finish: Boolean): LookupElementBuilder = this.withInsertHandler { ctx, _ ->
  ctx.document.insertString(ctx.selectionEndOffset, if (finish) "();" else "()")
  EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
}
