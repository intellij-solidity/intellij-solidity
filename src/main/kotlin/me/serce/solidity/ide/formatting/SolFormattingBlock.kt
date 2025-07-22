package me.serce.solidity.ide.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.IElementType
import me.serce.solidity.lang.core.SolidityTokenTypes.*
import java.util.*

open class SolFormattingBlock(
  private val astNode: ASTNode,
  private val alignment: Alignment?,
  private val indent: Indent,
  private val wrap: Wrap?,
  private val codeStyleSettings: CodeStyleSettings,
  private val spacingBuilder: SpacingBuilder,
  private val isEnforceChildIndent: Boolean,
  private val extra: Boolean
) : ASTBlock {
  private val nodeSubBlocks: List<Block> by lazy { buildSubBlocks().let { if (extra) it + SyntheticSolFormattingBlock(this) else it } }
  private val isNodeIncomplete: Boolean by lazy { FormatterUtil.isIncomplete(node) }
  private val doc: Document? by lazy { (astNode.psi)?.let { PsiDocumentManager.getInstance(it.project).getDocument(it.containingFile) } }

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

  private val binaryExpressionTypes = setOf(OR_EXPRESSION, AND_EXPRESSION, EQ_EXPRESSION, COMP_EXPRESSION, OR_OP_EXPRESSION,
    XOR_OP_EXPRESSION, AND_OP_EXPRESSION, SHIFT_EXPRESSION, PLUS_MIN_EXPRESSION, MULT_DIV_EXPRESSION, EXPONENT_EXPRESSION)

  private fun buildSubBlock(child: ASTNode): Block {
    var enforceChildIndent = isEnforceChildIndent
    val childType = child.elementType
    val type = astNode.elementType
    val parent = astNode.treeParent
    val parentType = parent?.elementType
    if (type == FUNCTION_INVOCATION || type == SEQ_EXPRESSION ||
      parent != null && type in binaryExpressionTypes &&
      doc?.let { it.getLineNumber(parent.startOffset) != it.getLineNumber(astNode.startOffset) } == true) {
      enforceChildIndent = false
    }
    val result = when {
      child is PsiComment && type in setOf(
        CONTRACT_DEFINITION,
        BLOCK,
        ENUM_DEFINITION,
        FUNCTION_DEFINITION,
        CONSTRUCTOR_DEFINITION,
        IF_STATEMENT,
        STRUCT_DEFINITION
      ) -> Indent.getNormalIndent()

      childType.isContractPart() -> Indent.getNormalIndent()

      // fields inside structs
      type == STRUCT_DEFINITION && childType == VARIABLE_DECLARATION -> Indent.getNormalIndent()

      // ternary expression
      childType == TERNARY_EXPRESSION -> Indent.getNormalIndent()
      type == TERNARY_EXPRESSION -> if (astNode.firstChildNode == child) Indent.getNoneIndent() else Indent.getNormalIndent()


      // inside a block, list of parameters, etc..
      parentType in setOf(BLOCK, UNCHECKED_BLOCK, ENUM_DEFINITION, YUL_BLOCK, PARAMETER_LIST, INDEXED_PARAMETER_LIST,
        MAP_EXPRESSION, SEQ_EXPRESSION, INLINE_ARRAY_EXPRESSION, TYPED_DECLARATION_LIST, RETURN_ST) -> Indent.getNormalIndent()

      // all expressions inside parens should have indentation when lines are split
      parentType in setOf(IF_STATEMENT, WHILE_STATEMENT, DO_WHILE_STATEMENT, FOR_STATEMENT) && childType != BLOCK -> {
        Indent.getNormalIndent()
      }

      // all function calls
      parentType in setOf(FUNCTION_INVOCATION, YUL_FUNCTION_CALL) -> Indent.getNormalIndent()

      // multi-line assign expression
      type in setOf(VARIABLE_DEFINITION, ASSIGNMENT_EXPRESSION) && astNode.lastChildNode.takeIf { it is CompositeElement && it != astNode.firstChildNode } == child -> {
        enforceChildIndent = true
        Indent.getNormalIndent()
      }

      type == FUNCTION_INVOCATION && parent?.treeParent?.elementType == MAP_EXPRESSION_CLAUSE && childType in setOf(FUNCTION_CALL_ARGUMENTS, LPAREN, RPAREN) -> Indent.getNormalIndent()

      else -> if (enforceChildIndent) Indent.getNormalIndent() else Indent.getNoneIndent()
    }
    val extra = parentType == TERNARY_EXPRESSION && (childType == QUESTION || childType == COLON) && parent?.treeParent?.elementType != VARIABLE_DEFINITION
    return SolFormattingBlock(child, alignment, result, null, codeStyleSettings, spacingBuilder, enforceChildIndent, extra)
  }

  private fun newChildIndent(childIndex: Int): Indent? = when {
    node.elementType in listOf(BLOCK, YUL_BLOCK, CONTRACT_DEFINITION, STRUCT_DEFINITION, ENUM_DEFINITION) -> {
      val lbraceIndex = subBlocks.indexOfFirst { it is ASTBlock && it.node?.elementType == LBRACE }
      if (lbraceIndex != -1 && lbraceIndex < childIndex) {
        Indent.getNormalIndent()
      } else {
        Indent.getNoneIndent()
      }
    }

    node.elementType == UNCHECKED_BLOCK -> Indent.getNormalIndent()
    node.elementType == TERNARY_EXPRESSION -> Indent.getNormalIndent()
    else -> Indent.getNoneIndent()
  }

  override fun getNode(): ASTNode = astNode
  override fun getTextRange(): TextRange = astNode.textRange
  override fun getWrap(): Wrap? = wrap
  override fun getIndent(): Indent? = indent
  override fun getAlignment(): Alignment? = alignment

  override fun getSpacing(child1: Block?, child2: Block): Spacing? {
    if ((child2 as? SolFormattingBlock)?.astNode?.elementType == COMMENT) {
      // SpacingBuilder does not allow to use the KeepingFirstColumnSpacing option, so calling it directly
      return Spacing.createKeepingFirstColumnSpacing(0, Int.MAX_VALUE, true, 1)
    }
    return spacingBuilder.getSpacing(this, child1, child2)
  }

  override fun getChildAttributes(newChildIndex: Int): ChildAttributes =
    ChildAttributes(newChildIndent(newChildIndex), null)

  override fun isIncomplete(): Boolean = isNodeIncomplete

  override fun isLeaf(): Boolean = astNode.firstChildNode == null

  // TODO nicer way to do the same
  private fun IElementType.isContractPart() = this in setOf(
    STATE_VARIABLE_DECLARATION,
    USING_FOR_DECLARATION,
    STRUCT_DEFINITION,
    MODIFIER_DEFINITION,
    FUNCTION_DEFINITION,
    CONSTRUCTOR_DEFINITION,
    EVENT_DEFINITION,
    ERROR_DEFINITION,
    ENUM_DEFINITION,
    USER_DEFINED_VALUE_TYPE_DEFINITION
  )
}

class SyntheticSolFormattingBlock(real: SolFormattingBlock) : Block by real {
  override fun getSubBlocks(): List<Block> = emptyList()
}
