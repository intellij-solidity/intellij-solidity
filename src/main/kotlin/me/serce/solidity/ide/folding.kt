package me.serce.solidity.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.CustomFoldingProvider
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.core.SolidityTokenTypes

class SolidityFoldingBuilder : CustomFoldingBuilder(), DumbAware {
  override fun buildLanguageFoldRegions(
    descriptors: MutableList<FoldingDescriptor>,
    root: PsiElement,
    document: Document,
    quick: Boolean
  ) {
    collectDescriptorsRecursively(root.node, document, descriptors)
  }

  override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String {
    val type = node.elementType
    return when (type) {
      SolidityTokenTypes.BLOCK -> "{...}"
      SolidityTokenTypes.COMMENT,
      SolidityTokenTypes.NAT_SPEC_TAG -> "/*...*/"

      SolidityTokenTypes.CONTRACT_DEFINITION,
      SolidityTokenTypes.ENUM_DEFINITION,
      SolidityTokenTypes.STRUCT_DEFINITION,
      SolidityTokenTypes.FUNCTION_DEFINITION -> "${node.text.substringBefore("{")} {...} "

      else -> "..."
    }
  }

  override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
    return false
  }

  companion object {

    private fun collectDescriptorsRecursively(
      node: ASTNode,
      document: Document,
      descriptors: MutableList<FoldingDescriptor>
    ) {
      val type = node.elementType
      if (
        type === SolidityTokenTypes.BLOCK && spanMultipleLines(node, document) ||
        type === SolidityTokenTypes.COMMENT ||
        type === SolidityTokenTypes.NAT_SPEC_TAG ||
        type === SolidityTokenTypes.CONTRACT_DEFINITION && spanMultipleLines(node, document) ||
        type === SolidityTokenTypes.STRUCT_DEFINITION && spanMultipleLines(node, document) ||
        type === SolidityTokenTypes.ENUM_DEFINITION && spanMultipleLines(node, document) ||
        type === SolidityTokenTypes.FUNCTION_DEFINITION &&
        node.findChildByType(SolidityTokenTypes.PARAMETER_LIST)?.let { spanMultipleLines(it, document) } == true) {

        // NatSpec has to be handled separately to ensure the whole block is folded
        if (type === SolidityTokenTypes.COMMENT || type === SolidityTokenTypes.NAT_SPEC_TAG) {
          val prevElementType = node.treePrev?.elementType
          if (prevElementType != SolidityTokenTypes.COMMENT //
            && prevElementType != SolidityTokenTypes.NAT_SPEC_TAG
          ) {
            var last = node
            var next = node.treeNext
            while (next != null && (next.elementType == SolidityTokenTypes.COMMENT //
                || next.elementType == SolidityTokenTypes.NAT_SPEC_TAG)
            ) {
              last = next
              next = next.treeNext
            }
            val range = TextRange(node.startOffset, last.textRange.endOffset)
            descriptors.add(FoldingDescriptor(node, range))
          }
        } else {
          descriptors.add(FoldingDescriptor(node, node.textRange))
        }
      }
      for (child in node.getChildren(null)) {
        collectDescriptorsRecursively(child, document, descriptors)
      }
    }

    private fun spanMultipleLines(node: ASTNode, document: Document): Boolean {
      val textRange = node.textRange
      return document.getLineNumber(textRange.startOffset) < document.getLineNumber(textRange.endOffset)
    }
  }
}

/// copied from com.intellij.lang.customFolding.VisualStudioCustomFoldingProvider
/// using the original class leads to "PluginException: Created extension classloader is not equal to plugin's one." error:
/// https://youtrack.jetbrains.com/issue/KMA-437
class VisualStudioCustomFoldingProvider : CustomFoldingProvider() {
  override fun isCustomRegionStart(elementText: String): Boolean {
    return elementText.contains("region") && elementText.matches("[/*#-]*\\s*region.*".toRegex())
  }

  override fun isCustomRegionEnd(elementText: String): Boolean {
    return elementText.contains("endregion") && elementText.matches("[/*#-]*\\s*endregion.*".toRegex())
  }

  override fun getPlaceholderText(elementText: String): String {
    val textAfterMarker = elementText.replaceFirst("[/*#-]*\\s*region(.*)".toRegex(), "$1")
    val result = if (elementText.startsWith("/*")) StringUtil.trimEnd(textAfterMarker, "*/")
      .trim { it <= ' ' } else textAfterMarker.trim { it <= ' ' }
    return if (result.isEmpty()) "..." else result
  }

  override fun getDescription(): String {
    return "region...endregion Comments"
  }

  override fun getStartString(): String {
    return "region ?"
  }

  override fun getEndString(): String {
    return "endregion"
  }
}

