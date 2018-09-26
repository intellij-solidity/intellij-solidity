package me.serce.solidity.lang.completion

import com.google.common.base.Joiner
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import com.intellij.util.ProcessingContext
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolParameterDef
import me.serce.solidity.lang.psi.SolParameterList
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.inferDeclType

class SolSimpleFunctionCompletionProvider(private vararg val functions: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
    functions
      .map {
        LookupElementBuilder.create(it).withTailText("()")
          .withInsertHandler { context, _ ->
            context.document.insertString(context.selectionEndOffset, "()")
            EditorModificationUtil.moveCaretRelatively(context.editor, 1)
          }
      }
      .forEach { result.addElement(it.keywordPrioritised()) }
  }
}

class SolFunctionCompletionContributor : CompletionContributor(), DumbAware {
  init {
    extend(CompletionType.BASIC, rootDeclaration(), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
        val pragmaBuilder = LookupElementBuilder
          .create("pragma solidity")
          .bold()
          .withTailText(" ^...")
          .withInsertHandler { ctx, _ ->
            ctx.document.insertString(ctx.selectionEndOffset, " ^0.4.4;")
            EditorModificationUtil.moveCaretRelatively(ctx.editor, 9)
          }
        result.addElement(PrioritizedLookupElement.withPriority(pragmaBuilder, KEYWORD_PRIORITY - 5))
      }
    })

    extend(CompletionType.BASIC, statement(),
      SolSimpleFunctionCompletionProvider("assert", "addmod", "mulmod", "keccak256", "sha3", "sha256", "ripemd160",
        "ecrecover", "revert"))

    extend(CompletionType.BASIC, insideContract(), SolSimpleFunctionCompletionProvider("selfdestruct"))

    extend(CompletionType.BASIC, insideFunctionDefinition(),
      object : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext?, result: CompletionResultSet) {
          val position = parameters.originalPosition
          val contract = getParentOfType(position, SolFunctionDefinition::class.java)?.contract ?: return

          val defType = inferDeclType(contract)
          when (defType) {
            is SolContract -> {
              val ref = defType.ref
              (ref.collectSupers.flatMap { SolResolver.resolveTypeName(it) } + ref)
                .filterIsInstance<SolContractDefinition>()
                .flatMap { it.functionDefinitionList }
                .map {
                  LookupElementBuilder
                    .create(it)
                    .withBoldness(true)
                    .withIcon(SolidityIcons.FUNCTION)
                    .withTypeText(methodReturnType(it.parameterListList))
                    .withTailText(methodIncomingType(it.parameterListList))
                }
                .map { insertParens(it, false) }
                .forEach(result::addElement)
            }
          }
        }
      })
  }
}


private fun methodReturnType(params: List<SolParameterList>) = when (params.size) {
  2 -> "(" + Joiner.on(",").join((params.get(1).parameterDefList).map { p -> p.typeName.text }) + ")"
  else -> "()"
}

private fun methodIncomingType(params: List<SolParameterList>) = when (!params.isEmpty()) {
  true -> "(" +
    Joiner.on(", ").join(
      (params.get(0).parameterDefList).map { formatParam(it) }) +
    ")"
  else -> "()"
}

private fun formatParam(param: SolParameterDef): String {
  val id = param.identifier?.text
  val type = param.typeName.text
  if (id == null) {
    return type
  } else {
    return "$id: $type"
  }
}
