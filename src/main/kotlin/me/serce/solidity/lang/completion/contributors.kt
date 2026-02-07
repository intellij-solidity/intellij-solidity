package me.serce.solidity.lang.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.patterns.*
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ProcessingContext
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.ide.hints.SolArgumentsDescription
import me.serce.solidity.ide.hints.startOffset
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.ref.SolImportPathReference
import me.serce.solidity.lang.stubs.SolGotoClassIndex
import me.serce.solidity.removeQuotes
import java.io.File

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
            // Pass matcher prefix + invocation count so global type completion can stay cheap on autopopup.
            SolCompleter.completeLiteral(position, result.prefixMatcher.prefix, parameters.invocationCount)
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
            // Pass matcher prefix + invocation count so global type completion can stay cheap on autopopup.
            SolCompleter.completeLiteral(position, result.prefixMatcher.prefix, parameters.invocationCount)
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
          val elements = descriptions.flatMap { it.arguments.map { it.split(" ").last() }.toList() }.toSet() - defined
          val needComma = parameters.originalPosition?.elementType != SolidityTokenTypes.RBRACE && elements.size > 1
          val results = elements.map {
              LookupElementBuilder.create(it).withIcon(SolidityIcons.STATE_VAR).withInsertHandler { context, _ ->
                val parent = parameters.originalPosition?.parent
                if (parent != null && parent !is SolMapExpressionClause) {
                  val insert = ": ${if (needComma) "," else ""}"
                  context.document.insertString(context.selectionEndOffset, insert)
                  context.editor.caretModel.currentCaret.moveToOffset(context.selectionEndOffset - 1)
                  CodeStyleManager.getInstance(context.project).reformatText(parent.containingFile, parent.startOffset, parent.textRange.endOffset)
                }
              }
            }
          result.addAllElements(results)
        }
      }
    )
    extend(CompletionType.BASIC, pathImportExpression(),
          object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
              val fullText = parameters.originalPosition?.text ?: return
              val text = fullText.removeQuotes()
              val project = parameters.position.project
              val curFile = parameters.originalFile.virtualFile
              val knownPrefixes = listOf('.', '/', '"')

              val matcher = CamelHumpMatcher(text)
              val humpFiles = StubIndex.getInstance()
                .getAllKeys(SolGotoClassIndex.KEY, project)
                .filter { matcher.prefixMatches(it) }
                .flatMap {
                  StubIndex.getInstance().getContainingFilesIterator(
                    SolGotoClassIndex.KEY, it, project, GlobalSearchScope.projectScope(project)
                  ).asSequence()
                }

              var dirText = dirPartOf(text)
              val vPath = SolImportPathReference.findImportFile(curFile, dirText)

              val prefix = result.prefixMatcher.prefix.takeWhile { knownPrefixes.contains(it) }
              val dirNoPrefix = dirText.dropWhile { knownPrefixes.contains(it) }

              val fileChildren = vPath?.children.orEmpty()
                .filter { it.isDirectory || it.extension == SolidityFileType.defaultExtension }
                .map {
                  LookupElementBuilder.create(prefix + dirNoPrefix + it.name)
                    .withIcon(SolidityIcons.FILE_ICON)
                    .withInsertHandler(::handleInsertImportPath)
                }
              val humpLookup = humpFiles.map {
                val rel = if (it.path.contains("node_modules/")) it.path.substringAfter("node_modules/")
                else (VfsUtil.findRelativePath(curFile.parent, it, '/')
                  ?.let { if (!it.startsWith(".")) "./$it" else it } ?: it.path)
                LookupElementBuilder.create("\"$rel")
                  .withLookupString("\"${it.name}")
                  .withIcon(SolidityIcons.FILE_ICON)
              }
              result.addAllElements((fileChildren + humpLookup).toList())
            }
          }
        )
  }

  private fun dirPartOf(input: String): String {
    val isDir = File(input).isDirectory || input.endsWith("/")
    val base = if (isDir) input else input.substringBeforeLast("/", missingDelimiterValue = input)
    return if (base.endsWith("/")) base else "$base/"
  }

  private fun handleInsertImportPath(context: InsertionContext, item: LookupElement) {
    val doc = context.document
    val text = doc.charsSequence
    val end = context.tailOffset
    val file = context.file
    val currentStringLiteral = file.findElementAt(context.startOffset)

    if (currentStringLiteral != null) {
      val hasQuoteAfter = end < text.length && text[end] == '"'
      if (!hasQuoteAfter) {
        doc.insertString(context.tailOffset, "\"")
      }
    }
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

private fun <E> or(vararg patterns: ElementPattern<E>) = StandardPatterns.or(*patterns)

fun statement(): PsiElementPattern.Capture<PsiElement> = psiElement<PsiElement>()
  .inside(SolStatement::class.java)

private inline fun <reified I : PsiElement> psiElement(): PsiElementPattern.Capture<I> {
  return psiElement(I::class.java)
}

fun LookupElementBuilder.keywordPrioritised(): LookupElement = PrioritizedLookupElement.withPriority(this, KEYWORD_PRIORITY)

fun LookupElementBuilder.insertParenthesis(finish: Boolean): LookupElementBuilder = this.withInsertHandler { ctx, _ ->
  ctx.document.insertString(ctx.selectionEndOffset, if (finish) "();" else "()")
  EditorModificationUtil.moveCaretRelatively(ctx.editor, 1)
}
