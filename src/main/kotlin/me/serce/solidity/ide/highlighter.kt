package me.serce.solidity.ide

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.lang.core.SolidityLexer
import me.serce.solidity.lang.core.SolidityTokenTypes.*

class SolHighlighterFactory : SingleLazyInstanceSyntaxHighlighterFactory() {
  override fun createHighlighter() = SolHighlighter()
}

class SolHighlighter : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() = SolidityLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<out TextAttributesKey> {
    return pack(tokenMapping[tokenType])
  }

  companion object {
    private val tokenMapping: Map<IElementType, TextAttributesKey> = mapOf(
      COMMENT to SolColor.LINE_COMMENT,

      LBRACE to SolColor.BRACES,
      RBRACE to SolColor.BRACES,
      LBRACKET to SolColor.BRACKETS,
      RBRACKET to SolColor.BRACKETS,
      LPAREN to SolColor.PARENTHESES,
      RPAREN to SolColor.PARENTHESES,
      SEMICOLON to SolColor.SEMICOLON,

      SCIENTIFICNUMBER to SolColor.NUMBER,
      FIXEDNUMBER to SolColor.NUMBER,
      DECIMALNUMBER to SolColor.NUMBER,
      HEXNUMBER to SolColor.NUMBER,
      NUMBERUNIT to SolColor.NUMBER,

      STRINGLITERAL to SolColor.STRING
    ).plus(
      keywords().map { it to SolColor.KEYWORD }
    ).plus(
      literals().map { it to SolColor.KEYWORD }
    ).plus(
      operators().map { it to SolColor.OPERATION_SIGN }
    ).mapValues { it.value.textAttributesKey }

    private fun keywords() = setOf<IElementType>(
      IMPORT, AS, PRAGMA, NEW, DELETE, EMIT,
      CONTRACT, LIBRARY, INTERFACE, IS, STRUCT, FUNCTION, ENUM,
      PUBLIC, PRIVATE, INTERNAL, EXTERNAL, CONSTANT, PAYABLE, PURE, VIEW,
      IF, ELSE, FOR, WHILE, DO, BREAK, CONTINUE, THROW, USING, RETURN, RETURNS,
      MAPPING, EVENT, ANONYMOUS, MODIFIER, ASSEMBLY, BYTENUMTYPE, BYTESNUMTYPE,
      FIXEDNUMTYPE, INTNUMTYPE, UFIXEDNUMTYPE, UINTNUMTYPE, STRING, BOOL, ADDRESS,
      VAR, STORAGE, MEMORY, WEI, ETHER, SZABO, FINNEY, SECONDS, MINUTES, HOURS,
      DAYS, WEEKS, YEARS
    )

    private fun literals() = setOf<IElementType>(BOOLEANLITERAL)

    private fun operators() = setOf<IElementType>(
      NOT, ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, MULT_ASSIGN, DIV_ASSIGN, PERCENT_ASSIGN,
      PLUS, MINUS, MULT, DIV, EXPONENT, CARET,
      LESS, MORE, LESSEQ, MOREEQ,
      AND, ANDAND, OR, OROR,
      EQ, NEQ, TO,
      INC, DEC,
      TILDE, PERCENT,
      LSHIFT, RSHIFT,
      LEFT_ASSEMBLY, RIGHT_ASSEMBLY
    )
  }
}

private inline fun <reified T : Any?> T?.asArray(): Array<out T> = if (this == null) emptyArray() else arrayOf(this)
