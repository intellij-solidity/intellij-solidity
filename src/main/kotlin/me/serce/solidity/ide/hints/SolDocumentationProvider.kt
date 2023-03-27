package me.serce.solidity.ide.hints

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings
import me.serce.solidity.ide.SolHighlighter
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.getSolType

const val NO_VALIDATION_TAG = "@custom:no_validation"

fun PsiElement.comments(): List<PsiElement> {
  return CachedValuesManager.getCachedValue(this) {
    val nonSolElements = siblings(false, false)
      .takeWhile { it !is SolElement }.toList()
    val res = PsiDocumentManager.getInstance(project).getDocument(this.containingFile)?.let { document ->
      val tripleLines = nonSolElements.filter { it.text.startsWith("///") }.map { document.getLineNumber(it.textOffset) }.toSet()
      val tripleLineComments = nonSolElements.filter { tripleLines.contains(document.getLineNumber(it.startOffset)) }
      val blockComments = nonSolElements.dropWhile { it.elementType != SolidityTokenTypes.COMMENT || !it.text.contains("*/") }.toList().let { l ->
        (l.indexOfFirst { it.elementType == SolidityTokenTypes.COMMENT && it.text.startsWith("/**") }.takeIf { it >= 0 }?.let { l.subList(0, it + 1) }
          ?: emptyList())
      }
      tripleLineComments + blockComments
    } ?: emptyList()
    CachedValueProvider.Result.create(res, this.parent)
  }
}

class SolDocumentationProvider : AbstractDocumentationProvider() {
  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    if (element == null) return null
    val builder = StringBuilder()
    if (!builder.appendDefinition(element)) return null

    return builder.toString()
  }

  private val keywordColors = SolHighlighter.keywords().plus(SolHighlighter.types()).filterNot { it == SolidityTokenTypes.RETURN }.map { it.debugName }
    .plus(setOf("u?int(\\d+)", "u?fixed(\\d+)", "bytes?(\\d+)"))
    .joinToString("|", "\\b(", ")\\b").toRegex()
  private val col = SolColor.TYPE.textAttributesKey.defaultAttributes.foregroundColor
  private val typeRGB = "rgb(${col.red},${col.green},${col.blue})"
  private fun String.colorizeKeywords(): String {
    return this.replace(keywordColors) { it.value.colorizeKeyword() }
  }
  private fun String.colorizeKeyword()  = "<b style='color:$typeRGB'>${this}</b>"

  override fun generateDoc(elementOrNull: PsiElement?, originalElement: PsiElement?): String? {
    var element = elementOrNull ?: return null
    if (element is SolMemberAccessExpression) {
      element = SolResolver.resolveMemberAccess(element).filterIsInstance<SolFunctionDefinition>().firstOrNull() ?: return null
    }
    val builder = StringBuilder()
    if (!builder.appendDefinition(element)) return null
    val comments = element.comments()
    if (comments.isNotEmpty()) {
      builder.append(CONTENT_START)
      comments.reversed().mapIndexed { i, e ->
        var text = e.text.let {
          if (comments.size == 1) it.replace("/**", "").replace("*/", "")
          else if (i == 0) it.replace("/**", "") else if (i == comments.size - 1) it.replace("*/", "") else it
        }.replace("///", "")
        if (e.elementType == SolidityTokenTypes.NAT_SPEC_TAG) {
          text = (if (e.text == NO_VALIDATION_TAG) "" else "<br/>$GRAYED_START${text.substring(1)}:$GRAYED_END")
        }
        builder.append(text)
      }
      builder.append(CONTENT_END)
    }

    return builder.toString()
  }

  private fun StringBuilder.appendDefinition(element: PsiElement) : Boolean {
    return calcDefinition(element)?.let {
      append(DEFINITION_START)
      append(it.colorizeKeywords())
      append(DEFINITION_END)
      true
    } ?: false
  }

  private fun calcDefinition(element: PsiElement): String? {
    return when (element) {
      is SolContractDefinition -> element.doc()
      is SolStructDefinition ->  "struct " + element.identifier.idName()
      is SolFunctionDefinition -> element.doc()
      is SolParameterDef -> element.colorizedTypeText()
      is SolVariableDeclaration -> element.colorizedTypeText()
      is SolTypedDeclarationItem -> "${getSolType(element.typeName).toString().colorizeKeywords()} ${element.identifier.idName()}"
      is SolStateVariableDeclaration -> "${getSolType(element.typeName).toString().colorizeKeywords()} ${element.identifier.idName()}"
      is SolEnumDefinition -> element.doc()
      is SolEventDefinition -> element.doc()
      is SolErrorDefinition -> element.doc()
      is SolModifierDefinition -> element.doc()

      else -> null
    }
  }

  private fun PsiElement?.idName() = this?.text ?: "<no_name>"

  private val colorizedTypes = SolHighlighter.types() + SolidityTokenTypes.CONTRACT_DEFINITION + SolidityTokenTypes.STRUCT
  private fun PsiElement.colorizedTypeText() = descendantsOfType<LeafPsiElement>().joinToString("") { el -> el.text.let { if (el.elementType in colorizedTypes || el.parent.elementType == SolidityTokenTypes.USER_DEFINED_TYPE_NAME) it.colorizeKeyword() else it } }

  private fun List<PsiElement>?.doc(separator: String = ", ", prefix: String = "", postfix : String = ""): String {
    return takeIf { it?.isNotEmpty() ?: false }
      ?.joinToString(separator, prefix, postfix) { e -> e.colorizedTypeText() } ?: ""
  }
  private fun SolContractDefinition.doc() : String {
    return "${contractType.docName} ${identifier.idName()}" +
      inheritanceSpecifierList.doc(prefix = " is ")
  }

  private fun SolFunctionDefinition.doc() : String {
    return "${if (isConstructor) "constructor" else "function"} ${identifier.idName()}(${parameters.doc()}) ${functionVisibilitySpecifierList.doc(" ")} " +
      "${stateMutabilityList.doc(" ")} ${modifierInvocationList.doc(" ")} ${returns?.parameterDefList?.doc(", ", "returns (", ")") ?: ""}"
  }

  private fun SolEnumDefinition.doc() : String {
    return "enum ${identifier.idName()} { ${enumValueList.doc()} }"
  }

  private fun SolEventDefinition.doc() : String {
    return "event ${identifier.idName()} ${children.joinToString { it.text.colorizeKeywords() }}"
  }

  private fun SolErrorDefinition.doc() : String {
    return "error ${identifier.idName()} ${children.joinToString { it.text.colorizeKeywords() }}"
  }

  private fun SolModifierDefinition.doc() : String {
    return "modifier ${identifier.idName()}(${parameterList?.parameterDefList?.doc() ?: ""}) " +
      "${virtualSpecifierList.takeIf { it.isNotEmpty() }?.doc() ?: ""} ${overrideSpecifierList.takeIf { it.isNotEmpty() }?.doc() ?: ""}"
  }
}
