package me.serce.solidity.lang.core

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.LanguageUtil
import com.intellij.lang.LightPsiParser
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import me.serce.solidity.SolidityParser
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import me.serce.solidity.lang.stubs.SolidityFileStub

class SolidityParserDefinition : ParserDefinition {
  override fun createParser(project: Project?): PsiParser = SolidityParser()

  override fun createFile(viewProvider: FileViewProvider): PsiFile = SolidityFile(viewProvider)

  override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
    LanguageUtil.canStickTokensTogetherByLexer(left, right, SolidityLexer())

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

  override fun getCommentTokens(): TokenSet = COMMENTS

  override fun getFileNodeType(): IFileElementType = SolidityFileStub.Type

  override fun createLexer(project: Project?): Lexer = SolidityLexer()

  override fun createElement(node: ASTNode?): PsiElement = SolidityTokenTypes.Factory.createElement(node)

  companion object {
    val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    val COMMENTS: TokenSet = TokenSet.create(COMMENT, NAT_SPEC_TAG)
    val BINARY_OPERATORS: TokenSet = TokenSet.create(
      PLUS, MINUS, MULT, DIV, EXPONENT,
      ASSIGN, TO, EQ, NEQ,
      PLUS_ASSIGN, MINUS_ASSIGN, MULT_ASSIGN, DIV_ASSIGN, OR_ASSIGN, XOR_ASSIGN, AND_ASSIGN, LSHIFT_ASSIGN, RSHIFT_ASSIGN, PERCENT_ASSIGN,
      LESS, LESSEQ, MORE, MOREEQ, CARET, AND, ANDAND, OR, OROR,
      PERCENT, LSHIFT, RSHIFT, LEFT_ASSEMBLY, RIGHT_ASSEMBLY
    )
    val CONTROL_STRUCTURES: TokenSet = TokenSet.create(
      IF, ELSE, WHILE, FOR, DO
    )
  }
}


class YulParserDefinition : ParserDefinition {
  override fun createParser(project: Project?): PsiParser = YulParser()

  override fun createFile(viewProvider: FileViewProvider): PsiFile = YulFile(viewProvider)

  override fun spaceExistenceTypeBetweenTokens(left: ASTNode, right: ASTNode): ParserDefinition.SpaceRequirements =
    LanguageUtil.canStickTokensTogetherByLexer(left, right, SolidityLexer())

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

  override fun getCommentTokens(): TokenSet = SolidityParserDefinition.COMMENTS

  override fun getFileNodeType(): IFileElementType = YulFileElementType

  override fun createLexer(project: Project?): Lexer = SolidityLexer()

  override fun createElement(node: ASTNode?): PsiElement {
    val element = node?.let { SolidityTokenTypes.Factory.createElement(it) }
    return element ?: ASTWrapperPsiElement(node!!)
  }
}

class YulParser : PsiParser, LightPsiParser {
  override fun parse(root: com.intellij.psi.tree.IElementType, builder: PsiBuilder): ASTNode {
    parseLight(root, builder)
    return builder.treeBuilt
  }

  override fun parseLight(root: com.intellij.psi.tree.IElementType, builder: PsiBuilder) {
    // Grammar-Kit generates a single-root parser (SourceUnit) for Solidity.
    // For standalone .yul we need a different root, so we wrap the generated
    // parser rules with a tiny custom root driver instead of duplicating grammar.
    val adaptedBuilder = GeneratedParserUtilBase.adapt_builder_(
      root,
      builder,
      this,
      SolidityParser.EXTENDS_SETS_
    )
    // Use the same section/exit pattern as generated parsers to keep
    // PSI/AST construction and error recovery consistent.
    val marker = GeneratedParserUtilBase.enter_section_(adaptedBuilder, 0, GeneratedParserUtilBase._COLLAPSE_, null)
    val result = parseRoot(adaptedBuilder, 0)
    GeneratedParserUtilBase.exit_section_(
      adaptedBuilder,
      0,
      marker,
      root,
      result,
      true,
      GeneratedParserUtilBase.TRUE_CONDITION
    )
  }

  private fun parseRoot(builder: PsiBuilder, level: Int): Boolean {
    // Prefer Yul Object syntax if it matches; otherwise fall back to the
    // inline-assembly statement grammar to support legacy/standalone blocks.
    if (SolidityParser.YulObject(builder, level + 1)) {
      return true
    }
    while (!builder.eof()) {
      val pos = GeneratedParserUtilBase.current_position_(builder)
      if (!SolidityParser.YulStatement(builder, level + 1)) {
        if (builder.eof()) {
          break
        }
        // Advance one token to avoid infinite loops on malformed input.
        builder.advanceLexer()
      }
      // Empty-element guard to prevent non-progressing loops.
      if (!GeneratedParserUtilBase.empty_element_parsed_guard_(builder, "YulFile", pos)) {
        break
      }
    }
    return true
  }
}
