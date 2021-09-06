package me.serce.solidity.lang.core

import com.intellij.lang.*
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

object ParserUtil {
  const val MAX_RECURSION_LEVEL = 512

  @JvmStatic
  fun recursion_guard_(builder: PsiBuilder, level: Int, funcName: String): Boolean {
    if (level > MAX_RECURSION_LEVEL) {
      builder.mark().error("Maximum recursion level ($MAX_RECURSION_LEVEL) reached in '$funcName'")
      return false
    }
    return true
  }
}

class SolidityParserDefinition : ParserDefinition {
  override fun createParser(project: Project?): PsiParser = SolidityParser()

  override fun createFile(viewProvider: FileViewProvider): PsiFile = SolidityFile(viewProvider)

  override fun spaceExistanceTypeBetweenTokens(left: ASTNode?, right: ASTNode?): ParserDefinition.SpaceRequirements? =
    LanguageUtil.canStickTokensTogetherByLexer(left, right, SolidityLexer())

  override fun getStringLiteralElements(): TokenSet = TokenSet.EMPTY

  override fun getWhitespaceTokens(): TokenSet = WHITE_SPACES

  override fun getCommentTokens(): TokenSet = COMMENTS

  override fun getFileNodeType(): IFileElementType? = SolidityFileStub.Type

  override fun createLexer(project: Project?): Lexer = SolidityLexer()

  override fun createElement(node: ASTNode?): PsiElement = SolidityTokenTypes.Factory.createElement(node)

  companion object {
    val WHITE_SPACES: TokenSet = TokenSet.create(TokenType.WHITE_SPACE)
    val COMMENTS: TokenSet = TokenSet.create(SolidityTokenTypes.COMMENT)
    val BINARY_OPERATORS: TokenSet = TokenSet.create(
      PLUS,
      MINUS,
      MULT,
      DIV,
      EXPONENT,
      ASSIGN,
      TO,
      EQ,
      NEQ,
      PLUS_ASSIGN,
      MINUS_ASSIGN,
      MULT_ASSIGN,
      DIV_ASSIGN,
      OR_ASSIGN,
      XOR_ASSIGN,
      AND_ASSIGN,
      LSHIFT_ASSIGN,
      RSHIFT_ASSIGN,
      PERCENT_ASSIGN,
      LESS,
      LESSEQ,
      MORE,
      MOREEQ,
      CARET,
      AND,
      ANDAND,
      OR,
      OROR,
      PERCENT,
      LSHIFT,
      RSHIFT,
      LEFT_ASSEMBLY,
      RIGHT_ASSEMBLY
    )
    val CONTROL_STRUCTURES: TokenSet = TokenSet.create(
      IF, ELSE, WHILE, FOR, DO
    )
  }
}
