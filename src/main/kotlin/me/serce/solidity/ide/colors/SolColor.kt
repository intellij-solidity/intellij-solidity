package me.serce.solidity.ide.colors

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Defaults

enum class SolColor(humanName: String, default: TextAttributesKey) {
  LINE_COMMENT("Comments//Comment", Defaults.LINE_COMMENT),
  NAT_SPEC_TAG("Comments//NatSpecTag", Defaults.DOC_COMMENT_TAG),

  CONTRACT_NAME("Types//Contract name", Defaults.CLASS_NAME),
  STRUCT_NAME("Types//Struct name", Defaults.CLASS_NAME),
  ERROR_NAME("Types//Error name", Defaults.CLASS_NAME),
  EVENT_NAME("Types//Event name", Defaults.CLASS_NAME),
  ENUM_NAME("Types//Enum name", Defaults.CLASS_NAME),
  ENUM_VALUE("Types//Enum value", Defaults.STATIC_FIELD),
  TYPE("Types//Value type", Defaults.KEYWORD),
  USER_DEFINED_VALUE_TYPE("Types//User-defined value type", Defaults.CLASS_NAME),

  GLOBAL("Identifiers//Global", Defaults.GLOBAL_VARIABLE),
  CONSTANT("Identifiers//Constant", Defaults.STATIC_FIELD),
  STATE_VARIABLE("Identifiers//State variable", Defaults.INSTANCE_FIELD),

  FUNCTION_DECLARATION("Functions//Function declaration", Defaults.FUNCTION_DECLARATION),
  RECEIVE_FALLBACK_DECLARATION("Functions//Receive/Fallback declaration", Defaults.STATIC_METHOD),
  FUNCTION_CALL("Functions//Function call", Defaults.FUNCTION_CALL),
  GLOBAL_FUNCTION_CALL("Functions//Global function call", Defaults.GLOBAL_VARIABLE),

  BRACES("Other//Braces", Defaults.BRACES),
  BRACKETS("Other//Brackets", Defaults.BRACKETS),
  PARENTHESES("Other//Parentheses", Defaults.PARENTHESES),
  SEMICOLON("Other//Semicolon", Defaults.SEMICOLON),
  NUMBER("Other//Number", Defaults.NUMBER),
  STRING("Other//String", Defaults.STRING),
  KEYWORD("Other//Keyword", Defaults.KEYWORD),
  OPERATION_SIGN("Other//Operation signs", Defaults.OPERATION_SIGN),
  ;

  val textAttributesKey = TextAttributesKey.createTextAttributesKey("me.serce.solidity.$name", default)
  val attributesDescriptor = AttributesDescriptor(humanName, textAttributesKey)
}
