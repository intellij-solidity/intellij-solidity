package me.serce.solidity.ide.hints

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup.*
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.*
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.rd.util.getOrCreate
import me.serce.solidity.ide.SolHighlighter
import me.serce.solidity.ide.colors.SolColor
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.SolErrorDefMixin
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.resolve.function.SolFunctionResolver
import me.serce.solidity.lang.types.getSolType

const val NO_VALIDATION_TAG = "@custom:no_validation"

fun PsiElement.comments(): List<PsiElement> {
  return CachedValuesManager.getCachedValue(this) {
    val nonSolElements = siblings(false, false)
      .takeWhile { it !is SolElement }.toList()
    val isBuiltin = this.containingFile.virtualFile == null
    val res = (if (!isBuiltin) PsiDocumentManager.getInstance(project).getDocument(this.containingFile)?.let { document ->
      val tripleLines = nonSolElements.filter { it.text.startsWith("///") }.map { document.getLineNumber(it.textOffset) }.toSet()
      val tripleLineComments = nonSolElements.filter { tripleLines.contains(document.getLineNumber(it.startOffset)) }
      val blockComments = collectBlockComments(nonSolElements)
      tripleLineComments + blockComments
    } ?: emptyList()
    else {
      collectBlockComments(nonSolElements)
    }).filterIsInstance<PsiComment>()
    CachedValueProvider.Result.create(res, if (isBuiltin) ModificationTracker.NEVER_CHANGED else this.parent)
  }
}

private fun collectBlockComments(nonSolElements: List<PsiElement>): List<PsiElement> {
  val blockComments = nonSolElements.dropWhile { it.elementType != SolidityTokenTypes.COMMENT || !it.text.contains("*/") }.toList().let { l ->
    (l.indexOfFirst { it.elementType == SolidityTokenTypes.COMMENT && it.text.startsWith("/**") }.takeIf { it >= 0 }?.let { l.subList(0, it + 1) }
      ?: emptyList())
  }
  return blockComments
}

class SolDocumentationProvider : AbstractDocumentationProvider() {
  override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
    if (element == null) return null
    val builder = StringBuilder()
    if (!builder.appendDefinition(element)) return null

    return builder.toString()
  }

  private val keywordColors = SolHighlighter.keywords().plus(SolHighlighter.types()).filterNot { it == SolidityTokenTypes.RETURN }.map { it.debugName }
    .plus(setOf("u?int(\\d+)", "u?fixed(\\d+)", "bytes?(\\d+)", "error"))
    .joinToString("|", "\\b(", ")\\b").toRegex()
  private val col = SolColor.TYPE.textAttributesKey.defaultAttributes.foregroundColor
  private val typeRGB = "rgb(${col.red},${col.green},${col.blue})"
  private fun String.colorizeKeywords(): String {
    return this.replace(keywordColors) { it.value.colorizeKeyword() }
  }
  private fun String.colorizeKeyword()  = "<b style='color:$typeRGB'>${this}</b>"

  private val commentsRegex = "^/\\*\\*|\\*/$|^///".toRegex()

  override fun generateDoc(elementOrNull: PsiElement?, originalElement: PsiElement?): String? {
    var element = elementOrNull ?: return null
    if (element is SolMemberAccessExpression) {
      element = SolResolver.resolveMemberAccess(element).filterIsInstance<SolFunctionDefinition>().firstOrNull() ?: return null
    }
    val builder = StringBuilder()
    if (!builder.appendDefinition(element)) return null
    data class DocWrapper(val document: Document?)
    val doc = mutableMapOf<PsiFile, DocWrapper>()

    fun getDoc(file: PsiFile) = doc.getOrCreate(file) { DocWrapper(PsiDocumentManager.getInstance(element.project).getDocument(file)) }.document
    val comments = element.comments().let {
      it + if (it.isEmpty() || it.any { it.elementType == SolidityTokenTypes.NAT_SPEC_TAG && it.text == "@inheritdoc" }) {
        when (element) {
          is SolParameterDef -> collectParameterComments(element) + collectInheritanceComments(element)
          is SolFunctionDefinition -> collectInheritanceComments(element)
          is SolStateVariableDeclaration -> collectInheritanceComments(element)
          else -> emptyList()
        }
      } else emptyList()
    }.reversed()
    if (comments.isNotEmpty()) {
      builder.append(CONTENT_START)
      var prevText = ""
      var text = comments.mapIndexed { i, e ->
        var text = e.text

        text = text.replace(commentsRegex, "")

        text = when (e.elementType) {
            SolidityTokenTypes.NAT_SPEC_TAG -> {
              if (e.text == NO_VALIDATION_TAG) "" else "$GRAYED_START${text.substring(1)}:$GRAYED_END"
            }
            SolidityTokenTypes.COMMENT -> {
              // replacing internal '*' characters which start a line
              val split = text.split("*").filter { it.isNotEmpty() }
              split.foldIndexed("") {i, a, t -> if (i == 0) a + t else {
                  a + (if (split[i - 1].lastOrNull { it == '\n' || !it.isWhitespace() } == '\n') "" else "*") + t
                } }
            }
            else -> text
        }
        text = text.split("\n").filter { it.contains("[^/]".toRegex()) }.joinToString("\n")
        getDoc(e.containingFile)?.let {doc ->
          if (i > 0 && (i > 1 || prevText.isNotBlank()) && doc.getLineNumber(e.textOffset) != doc.getLineNumber(comments[i - 1].endOffset)) {
            text = "\n" + text
          } else text
        }
        prevText = text
        text
      }.joinToString("")
      val split = text.split("\n")
      text = split.filterIndexed { i, l -> !((i == 0 || i == split.size - 1) && l.isBlank()) }.joinToString("\n")
      text = text.replace(" ", "&nbsp;").replace("\n", "<br/>")
      builder.append(text);
      builder.append(CONTENT_END)
    }

    return builder.toString()
  }

  private fun collectInheritanceComments(element: SolElement): List<PsiElement> {
    if (element !is SolNamedElement) return emptyList()
    val function = element.parentOfType<SolFunctionDefinition>(false)
      ?: element.parentOfType<SolStateVariableDeclaration>(false)?.let { SolPsiFactory(element.project).createFunction("function ${element.name}() {}") }
      ?: return emptyList()

    fun findTargetElement(base: SolFunctionDefinition) =  (when (element) {
          is SolFunctionDefinition -> base.comments()
          is SolParameterDef -> base.parameters.find { it.identifier?.text == element.identifier?.text }?.let { collectParameterComments(it) }
          is SolStateVariableDeclaration -> base.comments()
          else -> null
        }) ?: emptyList()

    element.comments().find { it.elementType == SolidityTokenTypes.NAT_SPEC_TAG && it.text == "@inheritdoc" }?.let {
      it.siblings().firstOrNull { it.elementType == SolidityTokenTypes.COMMENT }?.let {
        val ref = it.text.trimStart().split("\\s".toRegex(), limit = 2)[0]
        SolFunctionResolver.collectOverriden(function, element.parentOfType(false)).find { it.contract?.identifier?.text == ref }?.let {
          return findTargetElement(it)
        }
      }
    }

    val base = SolFunctionResolver.collectOverriden(function).takeIf { it.size == 1 }?.first() ?: return emptyList()
    if (function.parameters.mapNotNull { it.identifier?.text } != base.parameters.mapNotNull { it.identifier?.text }) return emptyList()
    return findTargetElement(base)
  }

  private fun collectParameterComments(element: SolParameterDef): List<PsiElement> {
    val functionDefinition = element.parentOfType<SolFunctionDefinition>() ?: return emptyList()
    val comments = functionDefinition.comments().takeIf { it.isNotEmpty() } ?: return emptyList()
    val natIndexes = comments.withIndex().filter { it.value.elementType == SolidityTokenTypes.NAT_SPEC_TAG }

    val returnParam = functionDefinition.returns?.parameterDefList?.contains(element) == true
    val natName = if (returnParam) "@return" else "@param"
    val singleReturn = returnParam && natIndexes.count { it.value.text == "@return" } == 1
    val paramName = element.identifier?.text ?: if (!singleReturn) return emptyList() else ""

    val natParam = natIndexes.find { it.value.text == natName && (singleReturn || comments.getOrNull(it.index - 1)?.text?.trimStart()?.startsWith(paramName) ?: false) } ?: return emptyList()
    val nextNatParam = natIndexes.getOrNull(natIndexes.indexOf(natParam) - 1)?.index?.let { it + 1 } ?: 0
    val paramComments = comments.slice(nextNatParam..natParam.index)
    return paramComments
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
      is SolUserDefinedValueTypeDefinition -> element.doc()
      is SolEnumValue -> element.idName()
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
    return ("${if (isConstructor) "constructor" else "function"} ${identifier.idName()}(${parameters.doc()}) ${functionVisibilitySpecifierList.doc(" ")} " +
      "${stateMutabilityList.doc(" ")} ${modifierInvocationList.doc(" ")} " +
      (returns?.parameterDefList?.doc(", ", "returns (", ")") ?: "")).replace("  ", " ")
  }

  private fun SolUserDefinedValueTypeDefinition.doc() : String {
    return "type ${identifier.idName()} is ${elementaryTypeName?.text}"
  }

  private fun SolEnumDefinition.doc() : String {
    return "enum ${identifier.idName()} { ${enumValueList.doc()} }"
  }

  private fun SolEventDefinition.doc() : String {
    return "event ${identifier.idName()} ${children.joinToString { it.text.colorizeKeywords() }}"
  }

  private fun SolErrorDefinition.doc() : String {
    return "error ${(this as? SolErrorDefMixin)?.nameIdentifier?.idName() ?: "N/A"} ${children.joinToString { it.text.colorizeKeywords() }}"
  }

  private fun SolModifierDefinition.doc() : String {
    return "modifier ${identifier.idName()}(${parameterList?.parameterDefList?.doc() ?: ""}) " +
      "${virtualSpecifierList.takeIf { it.isNotEmpty() }?.doc() ?: ""} ${overrideSpecifierList.takeIf { it.isNotEmpty() }?.doc() ?: ""}"
  }
}


