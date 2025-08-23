package me.serce.solidity.ide.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.lang.core.SolidityParserDefinition.Companion.BINARY_OPERATORS
import me.serce.solidity.lang.core.SolidityParserDefinition.Companion.CONTROL_STRUCTURES
import me.serce.solidity.lang.core.SolidityTokenTypes.*

/**
 * The formatter attempts to follow the official style guide while remaining configurable.
 * https://docs.soliditylang.org/en/latest/style-guide.html
 */
class SolidityFormattingModelBuilder : FormattingModelBuilder {
  override fun createModel(context: FormattingContext): FormattingModel {
    val element = context.psiElement
    val settings = context.codeStyleSettings
    val spacingBuilder = createSpacingBuilder(settings)

    val containingFile = element.containingFile
    val solidityBlock = SolFormattingBlock(element.node, null, Indent.getNoneIndent(), null, settings, spacingBuilder, false, false)

    return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, solidityBlock, settings)
  }

  override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? {
    return null
  }

  private val topLevelDeclarations =
    TokenSet.create(CONTRACT_DEFINITION, STRUCT_DEFINITION, ENUM_DEFINITION, ERROR_DEFINITION)

  private val openingBraces = TokenSet.create(LPAREN, LBRACE, LBRACKET)

  private val declarationOrParameterLists =
    TokenSet.create(VARIABLE_DECLARATION, PARAMETER_LIST, INDEXED_PARAMETER_LIST)

  private val controlFlowStatements =
    TokenSet.create(IF_STATEMENT, WHILE_STATEMENT, FOR_STATEMENT, DO_WHILE_STATEMENT)

  private val pragmaOrImport = TokenSet.create(PRAGMA_DIRECTIVE, IMPORT_DIRECTIVE)

  private val contractMembers = TokenSet.create(
    FUNCTION_DEFINITION,
    EVENT_DEFINITION,
    ERROR_DEFINITION,
    STRUCT_DEFINITION,
    STATE_VARIABLE_DECLARATION
  )

  fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(settings, SolidityLanguage)
      .between(openingBraces, COMMENT).spaces(1)
      .after(openingBraces).none()
      .between(MINUS, MORE).none()
      .before(TokenSet.create(RPAREN, RBRACE, RBRACKET, COMMA, SEMICOLON)).none()
      .around(BINARY_OPERATORS).spaces(1)
      .beforeInside(COLON, MAP_EXPRESSION_CLAUSE).spaces(0)
      .afterInside(COLON, MAP_EXPRESSION_CLAUSE).spaces(1)
      .around(TokenSet.create(QUESTION, COLON, IS)).spaces(1)
      .after(TokenSet.create(RETURNS, RETURN, IMPORT)).spaces(1)
      .after(MAPPING).spaces(0)
      .afterInside(TokenSet.create(NOT, TILDE, INC, DEC, PLUS, MINUS), UNARY_EXPRESSION).none()
      .afterInside(DELETE, UNARY_EXPRESSION).spaces(1)
      .afterInside(SEMICOLON, FOR_STATEMENT).spaces(1)
      .beforeInside(SEMICOLON, FOR_STATEMENT).none()
      .beforeInside(PARAMETER_LIST, FUNCTION_DEFINITION).none()
      .beforeInside(IDENTIFIER, FUNCTION_DEFINITION).spaces(1)
      .around(FUNCTION_INVOCATION).spaces(0)
      .aroundInside(MODIFIER_INVOCATION, FUNCTION_DEFINITION).spaces(1)
      .around(FUNCTION_VISIBILITY_SPECIFIER).spaces(1)
      .around(STATE_MUTABILITY_SPECIFIER).spaces(1)
      .betweenInside(MODIFIER_INVOCATION, MODIFIER_INVOCATION, FUNCTION_DEFINITION).spaces(1)
      .aroundInside(TO, MAPPING_TYPE_NAME).spaces(1)
      .aroundInside(
        TokenSet.create(
          FUNCTION_CALL_EXPRESSION, CONSTANT, EXTERNAL, PUBLIC, INTERNAL, PRIVATE, IDENTIFIER
        ),
        FUNCTION_DEFINITION
      ).spaces(1)
      .beforeInside(LPAREN, FUNCTION_CALL_EXPRESSION).none()
      .after(CONTROL_STRUCTURES).spaces(1)
      .beforeInside(STATEMENT, controlFlowStatements).spaces(1)
      .after(CONTRACT).spaces(1)
      .aroundInside(
        IDENTIFIER,
        TokenSet.create(CONTRACT_DEFINITION, PRAGMA_DIRECTIVE, STRUCT_DEFINITION, PARAMETER_DEF)
      ).spaces(1)
      .afterInside(TYPE_NAME, declarationOrParameterLists).spaces(1)
      .beforeInside(IDENTIFIER, declarationOrParameterLists).spaces(1)
      .after(COMMA).spaces(1)
      .beforeInside(BLOCK, STATEMENT).spaces(1)
      .beforeInside(UNCHECKED_BLOCK, STATEMENT).spaces(1)
      .beforeInside(LBRACE, CONTRACT_DEFINITION).spaces(1)
      // else on the same line as }
      .betweenInside(STATEMENT, ELSE, IF_STATEMENT).spaces(1)
      .between(STATEMENT, COMMENT).spacing(0, Int.MAX_VALUE, 0, true, 1)
      .after(STATEMENT).lineBreakInCode()
      .between(LBRACE, RBRACE).none()
      .afterInside(EXPRESSION, MEMBER_ACCESS_EXPRESSION).none()
      .beforeInside(IDENTIFIER, MEMBER_ACCESS_EXPRESSION).none()
      .between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).blankLines(0)
      // 0 lines between event definitions
      .between(EVENT_DEFINITION, EVENT_DEFINITION).blankLines(0)
      // 0 lines between error definitions
      .between(ERROR_DEFINITION, ERROR_DEFINITION).blankLines(0)
      // 1 line between pragma and import directive
      .between(PRAGMA_DIRECTIVE, TokenSet.create(IMPORT_DIRECTIVE))
      .blankLines(1)
      // 1 line between pragma/import and contract/struct definition
      .between(
        pragmaOrImport,
        topLevelDeclarations
      ).blankLines(1)
      // 1 line between contracts/structs
      .between(
        topLevelDeclarations,
        topLevelDeclarations
      ).blankLines(1)
      // allow for 0 lines between state variable declarations
      .between(STATE_VARIABLE_DECLARATION, STATE_VARIABLE_DECLARATION).blankLines(0)
      // allow for 0 lines between state constant variable declarations
      .between(CONSTANT_VARIABLE_DECLARATION, CONSTANT_VARIABLE_DECLARATION).blankLines(0)
      .between(contractMembers, contractMembers).blankLines(1)
  }
}
