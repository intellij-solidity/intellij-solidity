package me.serce.solidity.ide.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.tree.IElementType
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import java.util.*
import kotlin.collections.ArrayList


class SolFormattingBlock(private val astNode: ASTNode,
                         private val alignment: Alignment?,
                         private val indent: Indent,
                         private val wrap: Wrap?,
                         private val codeStyleSettings: CodeStyleSettings,
                         private val spacingBuilder: SpacingBuilder) : ASTBlock {
  private val nodeSubBlocks: List<Block> by lazy { buildSubBlocks() }
  private val isNodeIncomplete: Boolean by lazy { FormatterUtil.isIncomplete(node) }

  override fun getSubBlocks(): List<Block> = nodeSubBlocks

  private fun buildSubBlocks(): List<Block> {
    val blocks = ArrayList<Block>()
    var child = astNode.firstChildNode
    while (child != null) {
      val childType = child.elementType
      if (child.textRange.length == 0) {
        child = child.treeNext
        continue
      }
      if (childType === TokenType.WHITE_SPACE) {
        child = child.treeNext
        continue
      }
      val e = buildSubBlock(child)
      blocks.add(e)
      child = child.treeNext
    }
    return Collections.unmodifiableList(blocks)
  }

  private fun buildSubBlock(child: ASTNode): Block {
    val indent = calcIndent(child)
    return SolFormattingBlock(child, alignment, indent, null, codeStyleSettings, spacingBuilder)
  }

  private fun calcIndent(child: ASTNode): Indent {
    val childType = child.elementType
    val type = astNode.elementType
    val parentType = astNode.treeParent?.elementType
    val result = when {
      child is PsiComment && type in listOf(CONTRACT_DEFINITION, BLOCK, FUNCTION_DEFINITION, STRUCT_DEFINITION) -> Indent.getNormalIndent()
      childType.isContractPart() -> Indent.getNormalIndent()
      type == STRUCT_DEFINITION && childType == VARIABLE_DECLARATION -> Indent.getNormalIndent()
      // inside a block, list of parameters, etc..
      parentType in listOf(BLOCK, INLINE_ASSEMBLY_BLOCK, PARAMETER_LIST, INDEXED_PARAMETER_LIST) -> Indent.getNormalIndent()
      else -> Indent.getNoneIndent()
    }
    return result
  }

  private fun newChildIndent(childIndex: Int): Indent? = when {
    node.elementType in listOf(BLOCK, CONTRACT_DEFINITION) -> {
      val lbraceIndex = subBlocks.indexOfFirst { it is ASTBlock && it.node.elementType == LBRACE }
      if (lbraceIndex != -1 && lbraceIndex < childIndex) {
        Indent.getNormalIndent()
      } else {
        Indent.getNoneIndent()
      }
    }
    else -> Indent.getNoneIndent()
  }


  override fun getNode(): ASTNode = astNode
  override fun getTextRange(): TextRange = astNode.textRange
  override fun getWrap(): Wrap? = wrap
  override fun getIndent(): Indent? = indent
  override fun getAlignment(): Alignment? = alignment

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    return spacingBuilder.getSpacing(this, child1, child2)
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes =
    ChildAttributes(newChildIndent(newChildIndex), null)

  override fun isIncomplete(): Boolean = isNodeIncomplete

  override fun isLeaf(): Boolean = astNode.firstChildNode == null

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

