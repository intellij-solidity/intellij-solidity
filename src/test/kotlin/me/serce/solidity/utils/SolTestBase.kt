package me.serce.solidity.utils

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.lang.annotations.Language

abstract class SolTestBase : SolLightPlatformCodeInsightFixtureTestCase() {
  inner class InlineFile(@Language("Solidity") private val code: String, val name: String = "ctr.sol") {
    private val hasCaretMarker = "/*caret*/" in code

    init {
      myFixture.configureByText(name, replaceCaretMarker(code))
    }

    fun withCaret() {
      check(hasCaretMarker) {
        "Please, add `/*caret*/` marker to\n$code"
      }
    }
  }

  protected val fileName: String
    get() = "${getTestName(true)}.sol"

  protected fun replaceCaretMarker(text: String) = text.replace("/*caret*/", "<caret>")

  inline fun <reified T : PsiElement> findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
    val caretMarker = "//$marker"
    val (elementAtMarker, data) = run {
      val text = myFixture.file.text
      val markerOffset = text.indexOf(caretMarker)
      check(markerOffset != -1) { "No `$marker` marker:\n$text" }
      check(text.indexOf(caretMarker, startIndex = markerOffset + 1) == -1) {
        "More than one `$marker` marker:\n$text"
      }

      val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
      val markerPosition = myFixture.editor.offsetToLogicalPosition(markerOffset + caretMarker.length - 1)
      val previousLine = LogicalPosition(markerPosition.line - 1, markerPosition.column)
      val elementOffset = myFixture.editor.logicalPositionToOffset(previousLine)
      myFixture.file.findElementAt(elementOffset)!! to data
    }
    val element = elementAtMarker.parentOfType<T>(strict = false)
      ?: error("No ${T::class.java.simpleName} at ${elementAtMarker.text}")
    return element to data
  }

  inline fun <reified T : PsiElement> PsiElement.parentOfType(strict: Boolean = true, minStartOffset: Int = -1): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

  inline fun <reified T : PsiElement> findElementInEditor(marker: String = "^"): T {
    val (element, data) = findElementAndDataInEditor<T>(marker)
    check(data.isEmpty()) { "Did not expect marker data" }
    return element
  }

  protected fun checkByFile(ignoreTrailingWhitespace: Boolean = true, action: () -> Unit) {
    val (before, after) = (fileName to fileName.replace(".sol", "After.sol"))
    myFixture.configureByFile(before)
    action()
    myFixture.checkResultByFile(after, ignoreTrailingWhitespace)
  }
}
