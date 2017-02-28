package me.serce.solidity.ide

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.formatting.FormattingModelProvider
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.formatting.SpacingBuilder
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.TokenSet
import me.serce.solidity.lang.core.SolidityParserDefinition.Companion.BINARY_OPERATORS
import me.serce.solidity.lang.core.SolidityParserDefinition.Companion.CONTROL_STRUCTURES

/**
 * Ideally we should fully implement this
 * https://github.com/ethereum/solidity/blob/develop/docs/style-guide.rst
 */
class SolidityFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
    val spacingBuilder = createSpacingBuilder(settings)

    val containingFile = element.containingFile
    val solidityBlock = SolidityFormattingBlock(element.node, null, Indent.getNoneIndent(), null, settings, spacingBuilder)

    return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, solidityBlock, settings)
  }

  override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? {
    return null
  }

  companion object {
    fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
      val SOURCE_UNIT = TokenSet.create(CONTRACT_DEFINITION, IMPORT_DIRECTIVE, PRAGMA_DIRECTIVE)
      return SpacingBuilder(settings, SolidityLanguage)
        .after(LPAREN).none()
        .before(RPAREN).none()
        .after(LBRACE).none()
        .before(RBRACE).none()
        .after(LBRACKET).none()
        .before(RBRACKET).none()
        .before(COMMA).none()
        .before(SEMICOLON).none()
        .around(BINARY_OPERATORS).spaces(1)
        .around(QUESTION).spaces(1)
        .around(COLON).spaces(1)
        .beforeInside(EXPRESSION, UNARY_EXPRESSION).none()
        .beforeInside(PARAMETER_LIST, FUNCTION_DEFINITION).none()
        .after(CONTROL_STRUCTURES).spaces(1)
        .after(CONTRACT).spaces(1)
        .aroundInside(IDENTIFIER, CONTRACT_DEFINITION).spaces(1)
        .after(STATEMENT).lineBreakInCode()
        .between(SOURCE_UNIT, SOURCE_UNIT).blankLines(2)
        .between(CONTRACT_PART, CONTRACT_PART).blankLines(1)
    }
  }
}
