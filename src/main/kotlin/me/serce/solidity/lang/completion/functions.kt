package me.serce.solidity.lang.completion

import com.google.common.base.Joiner
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil.findElementOfClassAtOffset
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import com.intellij.util.ProcessingContext
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolInternalTypeFactory

class SolFunctionCompletionContributor : CompletionContributor(), DumbAware {

  init {
    extend(CompletionType.BASIC, expression(), FunctionCompletionProvider)
  }

  override fun beforeCompletion(context: CompletionInitializationContext) {
    val file = context.file
    if (file is SolidityFile) {
      if (context.completionType == CompletionType.BASIC) {
        val dummySuffix = deduceSemicolonOrBracket(context.editor, file, context.startOffset)
        context.dummyIdentifier = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED + dummySuffix
      }
    }
  }
}

private fun deduceSemicolonOrBracket(editor: Editor, file: SolidityFile, startOffset: Int): String {
  if (editor !is EditorEx) {
    return ""
  }

  val element = file.findElementAt(startOffset)

  val iterator = editor.highlighter.createIterator(startOffset)
  if (iterator.atEnd()) {
    return ""
  }

  if (iterator.tokenType === TokenType.WHITE_SPACE) {
    iterator.advance()
  }

  if (!iterator.atEnd() && iterator.tokenType === SolidityTokenTypes.IDENTIFIER) {
    iterator.advance()
  }

  if (!iterator.atEnd() && iterator.tokenType === SolidityTokenTypes.LPAREN
    && getParentOfType(element, SolFunctionCallExpression::class.java) != null
  ) {
    return "" // function call
  }

  if (!iterator.atEnd() && iterator.tokenType === SolidityTokenTypes.RPAREN) {
    iterator.advance()
  }

  if (!iterator.atEnd() && iterator.tokenType == SolidityTokenTypes.SEMICOLON) {
    return ""
  }

  // lets check where we are in the source code
  if (findElementOfClassAtOffset(file, startOffset, SolFunctionCallArguments::class.java, false) != null) {
    return ""
  }

  // it may be array access
  if (!iterator.atEnd() && iterator.tokenType === SolidityTokenTypes.RBRACKET) {
    return "];"
  }

  return ");"
}

object FunctionCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext?,
    result: CompletionResultSet
  ) {
    val position = parameters.originalPosition
    val contract = getParentOfType(position, SolFunctionDefinition::class.java)?.contract ?: return
    val globalRef = SolInternalTypeFactory.of(contract.project).globalType.ref
    val availableRefs = contract.collectSupers.flatMap { SolResolver.resolveTypeName(it) } + globalRef + contract
    availableRefs
      .filterIsInstance<SolContractDefinition>()
      .flatMap { it.functionDefinitionList + it.structDefinitionList }
      .filterIsInstance<PsiNamedElement>()
      .filterNot { it.name == null }
      .map {
        LookupElementBuilder
          .create(it as PsiNamedElement)
          .withBoldness(true)
          .withIcon(SolidityIcons.FUNCTION)
          .withTypeText(funcOutType(it))
          .withTailText(funcInType(it))
      }
      .map { insertParenthesis(it, false) }
      .forEach(result::addElement)
  }

}

private fun funcOutType(elem: PsiElement) = when (elem) {
  is SolFunctionDefinition -> {
    val params = elem.parameterListList
    when (params.size) {
      2 -> {
        val declaredOutTypes = params[1].parameterDefList.map { p -> p.typeName.text }
        val joinedParams = Joiner.on(",").join(declaredOutTypes)
        "($joinedParams)"
      }
      else -> ""
    }
  }
  is SolStructDefinition -> {
    val structName = elem.identifier?.text
    "($structName)"
  }
  else -> ""
}

private fun funcInType(elem: PsiElement) = when (elem) {
  is SolFunctionDefinition -> {
    val declaredInTypes = elem.parameterListList[0].parameterDefList.map { formatParam(it) }
    val joinedParams = Joiner.on(", ").join(declaredInTypes)
    "($joinedParams)"
  }
  is SolStructDefinition -> {
    val structFields = (elem.variableDeclarationList).map { v -> v.identifier?.text + ": " + v.typeName?.text }
    val joinedParams = Joiner.on(", ").join(structFields)
    "($joinedParams)"
  }
  else -> ""
}

private fun formatParam(param: SolParameterDef): String {
  val id = param.identifier?.text
  val type = param.typeName.text
  return when (id) {
    null -> type
    else -> "$id: $type"
  }
}
