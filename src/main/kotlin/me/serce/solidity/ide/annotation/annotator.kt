package me.serce.solidity.ide.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.tree.util.children
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.ide.hints.startOffset
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolErrorDefMixin
import me.serce.solidity.lang.resolve.SolResolver

class SolidityAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is SolElement) {
      highlight(element, holder)
    }
  }

  private fun highlight(element: SolElement, holder: AnnotationHolder) {
    fun keyword() = applyColor(holder, element, SolColor.KEYWORD)
    when (element) {
      is SolParameterDef -> element.identifier?.let {
        applyColor(
          holder, element.identifier!!, SolColor.FUNCTION_PARAMETER
        )
      }
      is SolImportDirective -> element.node.children().find { it.text == "from" }
        ?.let { applyColor(holder, it.textRange, SolColor.KEYWORD) }
      is SolNumberType -> applyColor(holder, element, SolColor.TYPE)
      is SolElementaryTypeName -> applyColor(holder, element, SolColor.TYPE)
      is SolStateMutabilitySpecifier -> if (element.text == "payable") keyword()
      is SolEnumValue -> applyColor(holder, element, SolColor.ENUM_VALUE)
      is SolMemberAccessExpression -> {
        when (element.expression.firstChild.text) {
          "super" -> applyColor(holder, element.expression.firstChild, SolColor.KEYWORD)
          "msg", "block", "abi" -> applyColor(holder, element.expression.firstChild, SolColor.GLOBAL)
        }

        if (element.reference?.resolve() is SolFunctionDefinition) {
          applyColor(holder, element.referenceNameElement, SolColor.FUNCTION_CALL)
        }
      }
      is SolErrorDefMixin -> {
        applyColor(holder, element.identifier, SolColor.KEYWORD)
        element.nameIdentifier?.let { applyColor(holder, it, SolColor.ERROR_NAME) }
      }
      is SolHexLiteral -> {
        if ((element.textRange.endOffset - element.startOffset) > 4) {
          applyColor(holder, TextRange(element.startOffset, element.startOffset + 3), SolColor.KEYWORD)
          applyColor(holder, TextRange(element.startOffset + 3, element.textRange.endOffset), SolColor.STRING)
        } else {
          applyColor(holder, element, SolColor.STRING)
        }
      }
      is SolRevertStatement -> applyColor(holder, element.firstChild, SolColor.KEYWORD)
      is SolOverrideSpecifier -> {
        if ((element.textRange.endOffset - element.startOffset) > 8) {
          applyColor(holder, TextRange(element.startOffset, element.startOffset + 8), SolColor.KEYWORD)
        }
      }
      is SolContractDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.CONTRACT_NAME) }
      is SolStructDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.STRUCT_NAME) }
      is SolEnumDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.ENUM_NAME) }
      is SolEventDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.EVENT_NAME) }
      is SolUserDefinedValueTypeDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.USER_DEFINED_VALUE_TYPE) }
      is SolConstantVariableDeclaration -> applyColor(holder, element.identifier, SolColor.CONSTANT)
      is SolStateVariableDeclaration -> {
        if (element.mutationModifier?.textMatches("constant") == true) {
          applyColor(holder, element.identifier, SolColor.CONSTANT)
        } else {
          applyColor(holder, element.identifier, SolColor.STATE_VARIABLE)
        }
      }
      is SolFunctionDefinition -> {
        val identifier = element.identifier
        if (identifier !== null) {
          applyColor(holder, identifier, SolColor.FUNCTION_DECLARATION)
        } else {
          val firstChildNode = element.node.firstChildNode
          if (firstChildNode.text == "receive" || firstChildNode.text == "fallback") {
            applyColor(holder, firstChildNode.textRange, SolColor.RECEIVE_FALLBACK_DECLARATION)
          }
        }
      }
      is SolModifierDefinition -> element.identifier?.let { applyColor(holder, it, SolColor.FUNCTION_DECLARATION) }
      is SolModifierInvocation -> applyColor(holder, element.varLiteral.identifier, SolColor.FUNCTION_CALL)
      is SolUserDefinedTypeName -> {
        when(SolResolver.resolveTypeNameUsingImports(element).firstOrNull()) {
          is SolContractDefinition -> applyColor(holder, element, SolColor.CONTRACT_NAME)
          is SolStructDefinition -> applyColor(holder, element, SolColor.STRUCT_NAME)
          is SolEnumDefinition -> applyColor(holder, element, SolColor.ENUM_NAME)
          is SolUserDefinedValueTypeDefinition -> applyColor(holder, element, SolColor.USER_DEFINED_VALUE_TYPE)
        }
      }
      is SolFunctionCallElement -> {
        when (element.firstChild.text) {
          "keccak256" -> applyColor(holder, element.firstChild, SolColor.GLOBAL_FUNCTION_CALL)
          "require" -> applyColor(holder, element.firstChild, SolColor.KEYWORD)
          "assert" -> applyColor(holder, element.firstChild, SolColor.KEYWORD)
          else -> when (SolResolver.resolveTypeNameUsingImports(element).firstOrNull()) {
            is SolErrorDefinition -> applyColor(holder, element.referenceNameElement, SolColor.ERROR_NAME)
            is SolEventDefinition -> applyColor(holder, element.referenceNameElement, SolColor.EVENT_NAME)
            else -> element.firstChild.let {
              if (it is SolPrimaryExpression && SolResolver.resolveTypeNameUsingImports(element.firstChild)
                  .filterIsInstance<SolStructDefinition>().isNotEmpty()
              ) {
                applyColor(holder, element.referenceNameElement, SolColor.STRUCT_NAME)
              } else if (element.referenceNameElement.reference?.resolve() is SolContractDefinition) {
                applyColor(holder, element.referenceNameElement, SolColor.CONTRACT_NAME)
              } else {
                applyColor(holder, element.referenceNameElement, SolColor.FUNCTION_CALL)
              }
            }
          }
        }
      }
      is SolYulVariableDeclaration, is SolYulSwitchStatement, is SolYulSwitchCase ->
        applyColor(holder, element.firstChild, SolColor.KEYWORD)
      is SolYulLeave, is SolYulBreak, is SolYulContinue, is SolYulDefault -> keyword()
      is SolYulFunctionCall -> applyColor(holder, element.firstChild, SolColor.FUNCTION_CALL)
      is SolLayoutAt -> keyword()
      is SolMutationModifier -> keyword() // transient
      is SolVarLiteral, is SolYulPath -> {
        if (additionalKeywordList().contains(element.text)) {
          applyColor(holder, element, SolColor.KEYWORD)
        } else if (globalKeywordList().contains(element.text)) {
          applyColor(holder, element, SolColor.GLOBAL)
        } else if (element.text == "keccak256") {
          applyColor(holder, element, SolColor.GLOBAL_FUNCTION_CALL)
        } else {
          when (element.reference?.resolve()) {
            is SolContractDefinition -> applyColor(holder, element, SolColor.CONTRACT_NAME)
            is SolFunctionDefinition -> applyColor(holder, element, SolColor.FUNCTION_CALL)
            is SolStateVarElement -> applyColor(holder, element, SolColor.STATE_VARIABLE)
            is SolParameterDef -> applyColor(holder, element, SolColor.FUNCTION_PARAMETER)
            is SolErrorDefinition -> applyColor(holder, element, SolColor.ERROR_NAME)
            is SolEventDefinition -> applyColor(holder, element, SolColor.EVENT_NAME)
            else -> {

            }
          }
        }
      }
    }
  }

  private fun additionalKeywordList(): List<String> {
    return listOf("this", "require", "assert","super")
  }

  private fun globalKeywordList(): List<String> {
    return listOf("msg", "block", "abi")
  }

  private fun applyColor(holder: AnnotationHolder, element: PsiElement, color: SolColor) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(element)
      .textAttributes(color.textAttributesKey)
      .create()
  }

  private fun applyColor(holder: AnnotationHolder, range: TextRange, color: SolColor) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(range)
      .textAttributes(color.textAttributesKey)
      .create()
  }
}
