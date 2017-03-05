package me.serce.solidity.ide

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import me.serce.solidity.lang.core.SolidityLexer
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

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
      COMMENT to Defaults.LINE_COMMENT,

      LBRACE to Defaults.BRACES,
      RBRACE to Defaults.BRACES,
      LBRACKET to Defaults.BRACKETS,
      RBRACKET to Defaults.BRACKETS,
      LPAREN to Defaults.PARENTHESES,
      RPAREN to Defaults.PARENTHESES,
      SEMICOLON to Defaults.SEMICOLON,


      DECIMALNUMBER to Defaults.NUMBER,
      HEXNUMBER to Defaults.NUMBER,
      NUMBERUNIT to Defaults.NUMBER,

      STRINGLITERAL to Defaults.STRING
    ).plus(
      keywords().map { it to Defaults.KEYWORD }
    ).plus(
      literals().map { it to Defaults.KEYWORD }
    ).plus(
      operators().map { it to Defaults.OPERATION_SIGN }
    ).mapValues { solidityKey(it.key, it.value) }

    private fun keywords() = setOf<IElementType>(
      IMPORT, AS, PRAGMA,
      CONTRACT, LIBRARY, IS, STRUCT, FUNCTION, ENUM,
      PUBLIC, PRIVATE, INTERNAL, EXTERNAL, CONSTANT, PAYABLE,
      IF, ELSE, FOR, WHILE, DO, BREAK, CONTINUE, THROW, USING, RETURN, RETURNS,
      MAPPING, EVENT, ANONYMOUS, MODIFIER, ASSEMBLY
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

private fun solidityKey(type: IElementType, key: TextAttributesKey) =
  TextAttributesKey.createTextAttributesKey("me.serce.solidity.$type", key)

private inline fun <reified T : Any?> T?.asArray(): Array<out T> = if (this == null) emptyArray() else arrayOf(this)
