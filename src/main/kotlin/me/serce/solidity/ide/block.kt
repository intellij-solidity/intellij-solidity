package me.serce.solidity.ide

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.util.containers.ContainerUtil
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import java.util.*


internal class SolidityFormattingBlock(private val astNode: ASTNode,
                                       private val alignment: Alignment?,
                                       private val indent: Indent,
                                       private val wrap: Wrap?,
                                       private val codeStyleSettings: CodeStyleSettings,
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
      blocks = buildSubBlocks();
    }
    return blocks as List<Block>;
  }

  private fun buildSubBlocks(): List<Block> {
    val blocks = ContainerUtil.newArrayList<Block>()
    var child = astNode.getFirstChildNode()
    while (child != null) {
      val childType = child.getElementType()
      if (child.getTextRange().getLength() == 0) {
        child = child.getTreeNext()
        continue
      }
      if (childType === TokenType.WHITE_SPACE) {
        child = child.getTreeNext()
        continue
      }
      val e = buildSubBlock(child)
      blocks.add(e)
      child = child.getTreeNext()
    }
    return Collections.unmodifiableList(blocks)
  }

  private fun buildSubBlock(child: ASTNode): Block {
    val indent = calcIndent(child)
    return SolidityFormattingBlock(child, alignment, indent, null, codeStyleSettings, spacingBuilder)
  }

  private fun calcIndent(child: ASTNode): Indent {
    val childType = child.elementType
    val type = astNode.elementType
    val parentType = astNode.treeParent?.elementType
    return when {
      child is PsiComment && (type == CONTRACT_DEFINITION || type == BLOCK || type == FUNCTION_DEFINITION) -> Indent.getNormalIndent()
      childType.isContractPart() -> Indent.getNormalIndent()
      parentType == BLOCK -> Indent.getNormalIndent()
      else -> return Indent.getNoneIndent()
    }
  }

  override fun getWrap(): Wrap? {
    return wrap
  }

  override fun getIndent(): Indent? {
    return indent
  }

  override fun getAlignment(): Alignment? {
    return alignment
  }

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    return spacingBuilder.getSpacing(this, child1, child2)
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
    var indent = Indent.getNoneIndent()
    return ChildAttributes(indent, null)
  }

  override fun isIncomplete(): Boolean {
    return false
  }

  override fun isLeaf(): Boolean {
    return astNode.firstChildNode == null
  }

  // TODO nicer way to do the same
  private fun IElementType.isContractPart() = this in listOf(
    STATE_VARIABLE_DECLARATION,
    USING_FOR_DECLARATION,
    STRUCT_DEFINITION,
    MODIFIER_DEFINITION,
    FUNCTION_DEFINITION,
    EVENT_DEFINITION,
    ENUM_DEFINITION
  )

}

