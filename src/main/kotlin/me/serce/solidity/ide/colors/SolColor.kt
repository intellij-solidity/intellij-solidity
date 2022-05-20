package me.serce.solidity.ide.colors

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class SolColor(humanName: String, default: TextAttributesKey) {
  LINE_COMMENT("Comment", Defaults.LINE_COMMENT),
  NAT_SPEC_TAG("NatSpecTag", Defaults.DOC_COMMENT_TAG),

  CONTRACT_NAME("Contract name", Defaults.CLASS_NAME),
  STRUCT_NAME("Struct name", Defaults.CLASS_NAME),
  ERROR_NAME("Error name", Defaults.CLASS_NAME),
  EVENT_NAME("Event name", Defaults.CLASS_NAME),

  CONSTANT_NAME("Constant name", Defaults.GLOBAL_VARIABLE),
  CONSTANT_STATE_VARIABLE_NAME("Constant state variable name", Defaults.STATIC_FIELD),
  STATE_VARIABLE_NAME("State variable name", Defaults.INSTANCE_FIELD),

  FUNCTION_DECLARATION("Function declaration", Defaults.FUNCTION_DECLARATION),
  FUNCTION_CALL("Function call", Defaults.FUNCTION_CALL),

  BRACES("Braces", Defaults.BRACES),
  BRACKETS("Brackets", Defaults.BRACKETS),
  PARENTHESES("Parentheses", Defaults.PARENTHESES),
  SEMICOLON("Semicolon", Defaults.SEMICOLON),

  NUMBER("Number", Defaults.NUMBER),
  STRING("String", Defaults.STRING),
  KEYWORD("Keyword", Defaults.KEYWORD),
  TYPE("Type", Defaults.KEYWORD),

  OPERATION_SIGN("Operation signs", Defaults.OPERATION_SIGN),
  CONTRACT_REFERENCE("Contract reference", Defaults.CLASS_REFERENCE),
  ;

  val textAttributesKey = TextAttributesKey.createTextAttributesKey("me.serce.solidity.$name", default)
  val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
}
