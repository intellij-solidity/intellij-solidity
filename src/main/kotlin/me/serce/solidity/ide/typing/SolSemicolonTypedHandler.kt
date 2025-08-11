package me.serce.solidity.ide.typing

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SolSemicolonTypedHandler : TypedHandlerDelegate() {
  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (c != ';' || !isSolidity(file)) return Result.CONTINUE

    val doc = editor.document
    val text = doc.charsSequence
    val caret = editor.caretModel.offset

    val closeParen = findNextClosingParen(text, caret, hardLimit = 4000) ?: return Result.CONTINUE

    val line = doc.getLineNumber(closeParen)
    val lineStart = doc.getLineStartOffset(line)
    val lineEnd = doc.getLineEndOffset(line)
    val lineSeg = text.subSequence(lineStart, lineEnd).toString()
    val commentIdxInLine = lineSeg.indexOf("//")
    val hardEnd = if (commentIdxInLine >= 0) lineStart + commentIdxInLine else lineEnd

    if (hasSemicolonBeforeEolSkippingBlockComments(text, closeParen + 1, hardEnd)) {
      return Result.CONTINUE
    }

    val nextLine = line + 1
    if (nextLine <= doc.lineCount - 1) {
      val ns = nextNonWs(text, doc.getLineStartOffset(nextLine), doc.getLineEndOffset(nextLine))
      if (ns >= 0 && text[ns] == ';') return Result.CONTINUE
    }

    var insertAt = hardEnd - 1
    while (insertAt >= lineStart && text[insertAt].isWhitespace() && text[insertAt] != '\n') insertAt--
    insertAt++ // after last non-space on that line (before // if any)

    val prev = prevNonWs(text, insertAt)
    if (prev >= 0 && text[prev] == ';') {
      return deleteTypedSemicolonOnly(project, editor, doc, caret)
    }

    // ==== FIX: adjust target index when deleting before it ====
    val delPos = caret - 1
    val willDelete = delPos >= 0 && delPos < text.length && text[delPos] == ';'
    var target = insertAt
    if (willDelete && delPos < insertAt) {
      target -= 1 // shift left because of the deletion before target
    }
    // ==========================================================

    WriteCommandAction.runWriteCommandAction(project) {
      if (willDelete) {
        doc.deleteString(delPos, delPos + 1)
      }
      doc.insertString(target, ";")
      editor.caretModel.moveToOffset(target + 1)
    }
    return Result.STOP
  }

  private fun isSolidity(file: PsiFile): Boolean =
    file.fileType.defaultExtension.equals("sol", true)

  private fun deleteTypedSemicolonOnly(
    project: Project,
    editor: Editor,
    doc: com.intellij.openapi.editor.Document,
    caret: Int
  ): Result {
    WriteCommandAction.runWriteCommandAction(project) {
      val t = doc.charsSequence
      val delPos = caret - 1
      if (delPos >= 0 && delPos < t.length && t[delPos] == ';') {
        doc.deleteString(delPos, delPos + 1)
      }
    }
    return Result.STOP
  }

  private fun findNextClosingParen(text: CharSequence, from: Int, hardLimit: Int): Int? {
    val end = (from + hardLimit).coerceAtMost(text.length)
    var i = from
    while (i < end) {
      when (val c = text[i]) {
        ')' -> return i
        ' ', '\t', '\r', '\n' -> {}
        '/' -> {
          if (i + 1 < end) {
            when (text[i + 1]) {
              '/' -> { i = skipToLineEnd(text, i + 2, end) - 1 }
              '*' -> { i = skipBlockComment(text, i + 2, end) - 1 }
              else -> return null
            }
          }
        }
        else -> return null
      }
      i++
    }
    return null
  }

  private fun hasSemicolonBeforeEolSkippingBlockComments(text: CharSequence, from: Int, to: Int): Boolean {
    var i = from.coerceAtLeast(0)
    val end = to.coerceAtMost(text.length)
    while (i < end) {
      when (val c = text[i]) {
        ';' -> return true
        '/' -> if (i + 1 < end && text[i + 1] == '*') { i = skipBlockComment(text, i + 2, end); continue }
      }
      i++
    }
    return false
  }

  private fun skipToLineEnd(text: CharSequence, from: Int, end: Int): Int {
    var i = from
    while (i < end && text[i] != '\n') i++
    return i
  }

  private fun skipBlockComment(text: CharSequence, from: Int, end: Int): Int {
    var i = from
    while (i + 1 < end) {
      if (text[i] == '*' && text[i + 1] == '/') return i + 2
      i++
    }
    return end
  }

  private fun nextNonWs(text: CharSequence, from: Int, to: Int): Int {
    var i = from
    val end = to.coerceAtMost(text.length)
    while (i < end) {
      if (!text[i].isWhitespace()) return i
      i++
    }
    return -1
  }

  private fun prevNonWs(text: CharSequence, from: Int): Int {
    var i = (from - 1).coerceAtMost(text.length - 1)
    while (i >= 0) {
      if (!text[i].isWhitespace()) return i
      i--
    }
    return -1
  }
}
