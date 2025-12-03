package me.serce.solidity.ide.typing

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate.Result
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import me.serce.solidity.lang.SolidityLanguage

class SolBlockCommentEnterHandler : EnterHandlerDelegateAdapter() {
  override fun postProcessEnter(file: PsiFile, editor: Editor, dataContext: DataContext): Result {
    if (file.language != SolidityLanguage) {
      // ensure only solidity files are affected, plugin.xml doesn't allow for registering
      // enterHandlerDelegate's for a specific language
      return Result.Continue
    }

    val document = editor.document
    val offset = editor.caretModel.offset
    val lineNumber = document.getLineNumber(offset)
    if (lineNumber == 0) {
      return Result.Continue
    }

    val lineStart = document.getLineStartOffset(lineNumber)
    val prevLineStart = document.getLineStartOffset(lineNumber - 1)
    val prevLineEnd = document.getLineEndOffset(lineNumber - 1)
    val prevLine = document.charsSequence.subSequence(prevLineStart, prevLineEnd).toString()
    val trimmedPrev = prevLine.trim()

    val lineEnd = document.getLineEndOffset(lineNumber)
    val currentLine = document.charsSequence.subSequence(lineStart, lineEnd).toString()
    val trimmedCurrent = currentLine.trim()

    val indent = document.charsSequence.subSequence(lineStart, offset).toString()
    val prevLineContainsCommentEnd = trimmedPrev.contains("*/")

    if (prevLineContainsCommentEnd) {
      return Result.Continue
    }

    return when {
      trimmedPrev.startsWith("/*") && trimmedCurrent == "*/" -> {
        document.replaceString(lineStart, lineEnd, "$indent * ")
        val insertionOffset = document.getLineEndOffset(lineNumber)
        document.insertString(insertionOffset, "\n$indent */")
        editor.caretModel.moveToOffset(lineStart + "$indent ".length + 2)
        Result.Stop
      }

      trimmedPrev.startsWith("/*") -> {
        document.insertString(offset, " * \n$indent */")
        editor.caretModel.moveToOffset(offset + 3)
        Result.Stop
      }

      trimmedPrev.startsWith("*") -> {
        var insertionOffset = offset
        if (prevLine.endsWith("* ")) {
          document.deleteString(prevLineEnd - 1, prevLineEnd)
          insertionOffset--
        }
        document.insertString(insertionOffset, "* ")
        editor.caretModel.moveToOffset(insertionOffset + 2)
        Result.Stop
      }

      else -> Result.Continue
    }
  }
}
