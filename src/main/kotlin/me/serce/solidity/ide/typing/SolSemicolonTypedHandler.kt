package me.serce.solidity.ide.typing

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.core.SolidityFile
import com.intellij.openapi.command.WriteCommandAction

class SolSemicolonTypedHandler : TypedHandlerDelegate() {
  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (c != ';' || file !is SolidityFile) {
      return Result.CONTINUE
    }

    val document = editor.document
    val text = document.charsSequence
    val offset = editor.caretModel.offset

    var i = offset
    while (i < text.length && text[i].isWhitespace()) i++
    if (i >= text.length || text[i] != ')') {
      return Result.CONTINUE
    }

    var j = i + 1
    while (j < text.length && text[j].isWhitespace() && text[j] != '\n') j++

    if (j < text.length) {
      if (text[j] == ';') {
        return Result.CONTINUE
      }
      if (text[j] == '\n') {
        var k = j + 1
        while (k < text.length && text[k].isWhitespace() && text[k] != '\n') k++
        if (k < text.length && text[k] == ';') {
          return Result.CONTINUE
        }
      }
    }

    WriteCommandAction.runWriteCommandAction(project) {
      document.deleteString(offset - 1, offset)
      if (j > offset - 1) {
        j--
      }
      val updated = document.charsSequence
      if (j >= updated.length || updated[j] != ';') {
        document.insertString(j, ";")
      }
    }
    editor.caretModel.moveToOffset(j + 1)
    return Result.STOP
  }
}
