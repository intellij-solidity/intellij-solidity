package me.serce.solidity.ide

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType

import java.util.ArrayList
import java.util.Collections

internal class SolidityBlock(private val astNode: ASTNode,
                         private val spacingBuilder: SpacingBuilder) : ASTBlock {
  private var blocks: List<Block>? = null

  override fun getNode(): ASTNode {
    return astNode
  }

  override fun getTextRange(): TextRange {
    return astNode.textRange
  }

  override fun getSubBlocks(): List<Block> {
    if (blocks == null) {
      blocks = buildSubBlocks()
    }
    return ArrayList(blocks!!)
  }

  private fun buildSubBlocks(): List<Block> {
    val myBlocks = ArrayList<Block>()
    var child: ASTNode? = astNode.firstChildNode
    while (child != null) {
      if (child.textRange.length == 0) {
        child = child.treeNext
        continue
      }

      if (child.elementType === TokenType.WHITE_SPACE) {
        child = child.treeNext
        continue
      }

      myBlocks.add(SolidityBlock(child, spacingBuilder))
      child = child.treeNext
    }
    return Collections.unmodifiableList(myBlocks)
  }

  override fun getWrap(): Wrap? {
    return null
  }

  override fun getIndent(): Indent? {
    return Indent.getNoneIndent()
  }

  override fun getAlignment(): Alignment? {
    return null
  }

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    return spacingBuilder.getSpacing(this, child1, child2)
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    return ChildAttributes(Indent.getNoneIndent(), null)
  }

  override fun isIncomplete(): Boolean {
    return false
  }

  override fun isLeaf(): Boolean {
    return astNode.firstChildNode == null
  }

}

