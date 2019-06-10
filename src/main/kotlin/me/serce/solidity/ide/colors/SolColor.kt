package me.serce.solidity.ide.colors

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class SolColor(humanName: String, default: TextAttributesKey) {
  LINE_COMMENT("Comment", Defaults.LINE_COMMENT),

  BRACES("Braces", Defaults.BRACES),
  BRACKETS("Brackets", Defaults.BRACKETS),
  PARENTHESES("Parentheses", Defaults.PARENTHESES),
  SEMICOLON("Semicolon", Defaults.SEMICOLON),

  NUMBER("Number", Defaults.NUMBER),
  STRING("String", Defaults.STRING),
  KEYWORD("Keyword", Defaults.KEYWORD),

  OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),
  CONTRACT_REFERENCE("Contract reference", Defaults.CLASS_REFERENCE),
  ;

  val textAttributesKey = TextAttributesKey.createTextAttributesKey("me.serce.solidity.$name", default)
  val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
}
