package me.serce.solidity.ide

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import me.serce.solidity.lang.core.SolidityTokenTypes
import java.util.*

class SolidityFoldingBuilder : FoldingBuilder, DumbAware {
  override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
    val descriptors = ArrayList<FoldingDescriptor>()
    collectDescriptorsRecursively(node, document, descriptors)
    return descriptors.toTypedArray()
  }

  override fun getPlaceholderText(node: ASTNode): String? {
    val type = node.elementType
    if (type === SolidityTokenTypes.BLOCK) {
      return "{...}"
    } else if (type === SolidityTokenTypes.COMMENT) {
      return "/*...*/"
    }
    return "..."
  }

  override fun isCollapsedByDefault(node: ASTNode): Boolean {
    return false
  }

  companion object {

    private fun collectDescriptorsRecursively(
      node: ASTNode,
      document: Document,
      descriptors: MutableList<FoldingDescriptor>
    ) {
      val type = node.elementType
      if ((type === SolidityTokenTypes.BLOCK) && spanMultipleLines(node, document)) {
        descriptors.add(FoldingDescriptor(node, node.textRange))
      } else if (type === SolidityTokenTypes.COMMENT) {
        descriptors.add(FoldingDescriptor(node, node.textRange))
      }

      for (child in node.getChildren(null)) {
        collectDescriptorsRecursively(child, document, descriptors)
      }
    }

    private fun spanMultipleLines(node: ASTNode, document: Document): Boolean {
      val range = node.textRange
      return document.getLineNumber(range.startOffset) < document.getLineNumber(range.endOffset)
    }
  }
}
