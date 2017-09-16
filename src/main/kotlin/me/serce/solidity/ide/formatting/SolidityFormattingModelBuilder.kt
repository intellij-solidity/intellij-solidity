package me.serce.solidity.ide.formatting

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
        .after(TokenSet.create(LPAREN, LBRACE, LBRACKET)).none()
        // Some old versions do not support .before(TokenSet), so we use more verbose form
        // https://github.com/JetBrains/intellij-community/commit/fd4c8224c17d041bf53d556f5c74ffaf20acffe3
        .before(RPAREN).none()
        .before(RBRACE).none()
        .before(RBRACKET).none()
        .before(COMMA).none()
        .before(SEMICOLON).none()
        .around(BINARY_OPERATORS).spaces(1)
        .around(TokenSet.create(QUESTION, COLON, IS)).spaces(1)
        .after(TokenSet.create(RETURNS, RETURN, IMPORT, MAPPING)).spaces(1)
        .afterInside(TokenSet.create(NOT, TILDE, INC, DEC, PLUS, MINUS), UNARY_EXPRESSION).none()
        .afterInside(DELETE, UNARY_EXPRESSION).spaces(1)
        .afterInside(SEMICOLON, FOR_STATEMENT).spaces(1)
        .beforeInside(SEMICOLON, FOR_STATEMENT).none()
        .beforeInside(PARAMETER_LIST, FUNCTION_DEFINITION).none()
        .beforeInside(IDENTIFIER, FUNCTION_DEFINITION).spaces(1)
        .aroundInside(FUNCTION_MODIFIER, FUNCTION_DEFINITION).spaces(1)
        .aroundInside(TO, MAPPING_TYPE_NAME).spaces(1)
        .aroundInside(TokenSet.create(
          FUNCTION_CALL_EXPRESSION, CONSTANT, PAYABLE,
          EXTERNAL, PUBLIC, INTERNAL, PRIVATE, IDENTIFIER),
          FUNCTION_DEFINITION).spaces(1)
        .beforeInside(LPAREN, FUNCTION_CALL_EXPRESSION).none()
        .after(CONTROL_STRUCTURES).spaces(1)
        .beforeInside(STATEMENT, TokenSet.create(IF_STATEMENT, WHILE_STATEMENT, FOR_STATEMENT, DO_WHILE_STATEMENT)).spaces(1)
        .after(CONTRACT).spaces(1)
        .aroundInside(IDENTIFIER, TokenSet.create(CONTRACT_DEFINITION, PRAGMA_DIRECTIVE, STRUCT_DEFINITION, PARAMETER_DEF)).spaces(1)
        .afterInside(TYPE_NAME, TokenSet.create(VARIABLE_DECLARATION, PARAMETER_LIST, INDEXED_PARAMETER_LIST)).spaces(1)
        .beforeInside(IDENTIFIER, TokenSet.create(VARIABLE_DECLARATION, PARAMETER_LIST, INDEXED_PARAMETER_LIST)).spaces(1)
        .after(COMMA).spaces(1)
        .beforeInside(BLOCK, STATEMENT).spaces(1)
        .beforeInside(LBRACE, CONTRACT_DEFINITION).spaces(1)
        .after(STATEMENT).lineBreakInCode()
        .between(LBRACE, RBRACE).none()
        .afterInside(EXPRESSION, MEMBER_ACCESS_EXPRESSION).none()
        .beforeInside(IDENTIFIER, MEMBER_ACCESS_EXPRESSION).none()
        .between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).blankLines(0)
        .between(SOURCE_UNIT, SOURCE_UNIT).blankLines(2)
        .between(SOURCE_UNIT, COMMENT).blankLines(2)
        .between(TokenSet.create(FUNCTION_DEFINITION, EVENT_DEFINITION, STRUCT_DEFINITION, STATE_VARIABLE_DECLARATION),
          TokenSet.create(FUNCTION_DEFINITION, EVENT_DEFINITION, STRUCT_DEFINITION, STATE_VARIABLE_DECLARATION)).blankLines(1)
    }
  }
}
