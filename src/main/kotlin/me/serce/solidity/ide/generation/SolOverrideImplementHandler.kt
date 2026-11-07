package me.serce.solidity.ide.generation

import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import me.serce.solidity.ide.inspections.fixes.AddVirtualFix
import me.serce.solidity.ide.inspections.fixes.SolSpecifierEditor
import me.serce.solidity.lang.SolidityFileType
import me.serce.solidity.lang.core.SolidityTokenTypes
import me.serce.solidity.lang.psi.ContractType
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.impl.LinearizationImpossibleException
import me.serce.solidity.lang.resolve.function.SolOverrideResolver
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings
import javax.swing.JTree

/** Shared logic of Code | Override Methods (Ctrl+O) and Code | Implement Methods (Ctrl+I) for Solidity. */
abstract class SolOverrideImplementHandler : LanguageCodeInsightActionHandler {
  protected abstract val title: String

  /** Whether a not-yet-overridden base member is offered by this action. */
  protected abstract fun accept(member: SolNamedElement): Boolean

  /** Hint shown when the contract has bases but nothing to offer. */
  protected abstract val nothingFoundMessage: String

  override fun startInWriteAction() = false

  override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
    // available whenever the contract inherits something, so an empty result can be explained with a hint
    val contract = contractAt(editor, file) ?: return false
    return SolContract(contract).getParents().isNotEmpty()
  }

  override fun invoke(project: Project, editor: Editor, file: PsiFile) {
    val contract = contractAt(editor, file) ?: return
    val candidates = candidates(contract)
    if (candidates.isEmpty()) {
      HintManager.getInstance().showErrorHint(editor, nothingFoundMessage)
      return
    }
    val chosen = choose(project, candidates)
    if (chosen.isNullOrEmpty()) return
    // non-virtual bases get `virtual` added, possibly in other files
    val files = (listOf(file) + chosen.mapNotNull { it.containingFile }).distinct().toTypedArray()
    WriteCommandAction.runWriteCommandAction(project, title, null, { insert(editor, contract, chosen) }, *files)
  }

  /** The contract whose body contains the caret; libraries can't inherit, so they are excluded. */
  fun contractAt(editor: Editor, file: PsiFile): SolContractDefinition? {
    val offset = editor.caretModel.offset
    val element = file.findElementAt(offset) ?: file.findElementAt(offset - 1) ?: return null
    val contract = PsiTreeUtil.getParentOfType(element, SolContractDefinition::class.java, false) ?: return null
    if (contract.contractType == ContractType.LIBRARY) return null
    val lbrace = contract.node.findChildByType(SolidityTokenTypes.LBRACE) ?: return null
    return if (offset >= lbrace.textRange.endOffset) contract else null
  }

  /** Most derived base declaration per signature, not yet redefined in [contract], filtered by [accept]. */
  fun candidates(contract: SolContractDefinition): List<SolNamedElement> {
    val parents = try {
      SolContract(contract).linearizeParents().map { it.ref }
    } catch (e: LinearizationImpossibleException) {
      return emptyList()
    }
    val own = SolOverrideResolver.overridableMembers(contract)
    val mostDerived = mutableListOf<SolNamedElement>()
    for (parent in parents) {
      for (member in SolOverrideResolver.overridableMembers(parent)) {
        if (!SolOverrideResolver.participates(member)) continue
        if (mostDerived.any { SolOverrideResolver.sameSignature(it, member) }) continue
        mostDerived += member
      }
    }
    return mostDerived.filter { m -> own.none { SolOverrideResolver.sameSignature(it, m) } && accept(m) }
  }

  /** Shows the member chooser; returns null when cancelled. In tests every candidate is chosen. */
  protected open fun choose(project: Project, candidates: List<SolNamedElement>): List<SolNamedElement>? {
    if (ApplicationManager.getApplication().isUnitTestMode) return candidates
    val chooser = MemberChooser(candidates.map { SolMemberChooserObject(it) }.toTypedArray(), false, true, project)
    chooser.title = title
    chooser.setCopyJavadocVisible(false)
    chooser.show()
    if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
    return chooser.selectedElements?.map { it.member }
  }

  fun insert(editor: Editor, contract: SolContractDefinition, members: List<SolNamedElement>) {
    val project = contract.project
    // bases that must become virtual; pointers survive the reparse caused by inserting the stubs
    val pointerManager = SmartPointerManager.getInstance(project)
    val toMakeVirtual = members.filter { !SolOverrideResolver.isVirtual(it) }.map { pointerManager.createSmartPsiElementPointer(it) }
    val document = editor.document
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    psiDocumentManager.doPostponedOperationsAndUnblockDocument(document)

    // indent ourselves: with an external formatter (Foundry) IntelliJ's formatter is disabled for Solidity
    val unit = indentUnit(project)
    val baseIndent = lineIndent(document, contract.textRange.startOffset) + unit
    val text = members.joinToString("\n\n") { m ->
      val stub = SolOverrideStubRenderer.render(m, SolOverrideResolver.visibleDeclaringBases(contract, m).mapNotNull { it.name })
      val lines = stub.lines()
      lines.mapIndexed { i, l -> if (i == 0 || i == lines.lastIndex) baseIndent + l else baseIndent + unit + l }
        .joinToString("\n")
    }

    var offset = insertionOffset(editor, contract)
    val line = document.getLineNumber(offset)
    val lineStart = document.getLineStartOffset(line)
    val lineEnd = document.getLineEndOffset(line)
    val inserted: String
    if (document.charsSequence.subSequence(lineStart, lineEnd).isBlank()) {
      // an empty line: replace it with the generated members
      document.deleteString(lineStart, lineEnd)
      offset = lineStart
      inserted = text
    } else if (document.charsSequence.subSequence(lineStart, offset).isBlank()) {
      // only indentation before the offset (e.g. before the closing brace)
      document.deleteString(lineStart, offset)
      offset = lineStart
      inserted = "$text\n" + lineIndent(document, contract.textRange.startOffset)
    } else {
      inserted = "\n$text\n"
    }
    document.insertString(offset, inserted)
    psiDocumentManager.commitDocument(document)
    val marker = document.createRangeMarker(offset, offset + inserted.length)
    if (SoliditySettings.getInstance(project).formatterType == FormatterType.INTELLIJ_SOLIDITY) {
      val file = psiDocumentManager.getPsiFile(document)
      if (file != null) {
        CodeStyleManager.getInstance(project).reformatText(file, marker.startOffset, marker.endOffset)
        psiDocumentManager.commitDocument(document)
      }
    }
    // place the caret at the start of the first generated member
    val firstLine = document.getLineNumber(marker.startOffset) + if (inserted.startsWith("\n")) 1 else 0
    if (firstLine < document.lineCount) {
      val start = document.getLineStartOffset(firstLine)
      val lineText = document.charsSequence.subSequence(start, document.getLineEndOffset(firstLine))
      editor.caretModel.moveToOffset(start + lineText.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0))
    }
    marker.dispose()

    for (pointer in toMakeVirtual) {
      pointer.element?.let { SolSpecifierEditor.addVirtual(project, it) }
    }
  }

  private fun lineIndent(document: Document, offset: Int): String {
    val start = document.getLineStartOffset(document.getLineNumber(offset))
    return document.charsSequence.subSequence(start, offset).takeWhile { it == ' ' || it == '\t' }.toString()
  }

  private fun indentUnit(project: Project): String {
    val options = CodeStyle.getProjectOrDefaultSettings(project).getIndentOptions(SolidityFileType)
    return if (options.USE_TAB_CHARACTER) "\t" else " ".repeat(options.INDENT_SIZE)
  }

  /** At the caret when it is between members of [contract]; after the member under the caret; else before `}`. */
  private fun insertionOffset(editor: Editor, contract: SolContractDefinition): Int {
    val rbrace = contract.node.findChildByType(SolidityTokenTypes.RBRACE)?.startOffset ?: contract.textRange.endOffset
    val lbraceEnd = contract.node.findChildByType(SolidityTokenTypes.LBRACE)?.textRange?.endOffset ?: return rbrace
    val caret = editor.caretModel.offset
    if (caret < lbraceEnd || caret > rbrace) return rbrace
    val member = contract.children.firstOrNull { it.textRange.startOffset < caret && caret < it.textRange.endOffset }
    return member?.textRange?.endOffset ?: caret
  }
}

class SolOverrideMethodsHandler : SolOverrideImplementHandler() {
  override val title = "Override Functions"
  override val nothingFoundMessage = "No functions to override"

  // non-virtual members are offered too and made virtual on insert, as long as their file can be edited
  override fun accept(member: SolNamedElement) = SolOverrideResolver.isVirtual(member) || AddVirtualFix.canEdit(member)
}

class SolImplementMethodsHandler : SolOverrideImplementHandler() {
  override val title = "Implement Functions"
  override val nothingFoundMessage = "No functions to implement"
  override fun accept(member: SolNamedElement) = member is SolFunctionDefinition && !SolOverrideResolver.hasBody(member)
}

private class SolMemberChooserObject(val member: SolNamedElement) : ClassMember {
  private val contract = SolOverrideResolver.containingContract(member)

  override fun getParentNodeDelegate(): MemberChooserObject? = contract?.let { SolContractChooserObject(it) }

  override fun renderTreeNode(component: SimpleColoredComponent, tree: JTree?) {
    component.append(text)
    if (!SolOverrideResolver.isVirtual(member)) {
      component.append("  will be made virtual", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
    component.icon = member.getIcon(0)
  }

  override fun getText(): String {
    val name = (member as? SolFunctionDefinition)?.specialFunction?.name?.lowercase() ?: member.name
    val params = when (member) {
      is SolFunctionDefinition -> member.parameterListList.firstOrNull()?.text
      is me.serce.solidity.lang.psi.SolModifierDefinition -> member.parameterList?.text
      else -> null
    } ?: "()"
    return "$name$params"
  }

  override fun equals(other: Any?) = other is SolMemberChooserObject && other.member == member
  override fun hashCode() = member.hashCode()
}

private class SolContractChooserObject(val contract: SolContractDefinition) :
  MemberChooserObjectBase(contract.name ?: "", contract.getIcon(0)) {
  override fun equals(other: Any?) = other is SolContractChooserObject && other.contract == contract
  override fun hashCode() = contract.hashCode()
}
