package me.serce.solidity.utils

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language

abstract class SolTestBase : SolLightPlatformCodeInsightFixtureTestCase() {
  inner class InlineFile(@Language("Solidity") private val code: String, val name: String = "ctr.sol") {
    val psiFile: PsiFile
    private val hasCaretMarker = "/*caret*/" in code

    init {
      psiFile = myFixture.configureByText(name, replaceCaretMarker(code))
    }

    fun withCaret() {
      check(hasCaretMarker) {
        "Please, add `/*caret*/` marker to\n$code"
      }
    }
  }

  protected val fileName: String
    get() = "${getTestName(true)}.sol"

  val fixture: CodeInsightTestFixture
    get() = super.myFixture

  protected fun replaceCaretMarker(text: String) = text.replace("/*caret*/", "<caret>")

  inline fun <reified T : PsiElement> findElementAndDataInEditor(marker: String = "^"): Pair<T, String> {
    val caretMarker = "//$marker"
    val (elementAtMarker, data) = run {
      val text = fixture.file.text
      val markerOffset = text.indexOf(caretMarker)
      check(markerOffset != -1) { "No `$marker` marker:\n$text" }
      check(text.indexOf(caretMarker, startIndex = markerOffset + 1) == -1) {
        "More than one `$marker` marker:\n$text"
      }

      val data = text.drop(markerOffset).removePrefix(caretMarker).takeWhile { it != '\n' }.trim()
      val markerPosition = fixture.editor.offsetToLogicalPosition(markerOffset + caretMarker.length - 1)
      val previousLine = LogicalPosition(markerPosition.line - 1, markerPosition.column)
      val elementOffset = fixture.editor.logicalPositionToOffset(previousLine)
      fixture.file.findElementAt(elementOffset)!! to data
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

  protected open fun checkEditorAction(
    @Language("Solidity") before: String,
    @Language("Solidity") after: String,
    actionId: String,
    trimIndent: Boolean = true,
  ) {
    checkByText(before.trimIndent(), after.trimIndent()) {
      myFixture.performEditorAction(actionId)
    }
  }

  protected fun checkByFile(ignoreTrailingWhitespace: Boolean = true, action: () -> Unit) {
    val (before, after) = (fileName to fileName.replace(".sol", "After.sol"))
    myFixture.configureByFile(before)
    action()
    myFixture.checkResultByFile(after, ignoreTrailingWhitespace)
  }

  protected fun checkByText(
    @Language("Solidity") before: String,
    @Language("Solidity") after: String,
    fileName: String = "main.sol",
    action: () -> Unit
  ) {
    InlineFile(before, fileName)
    action()
    PsiTestUtil.checkPsiStructureWithCommit(myFixture.file, PsiTestUtil::checkPsiMatchesTextIgnoringNonCode)
    myFixture.checkResult(replaceCaretMarker(after))
  }

  protected fun checkResult(@Language("Solidity") text: String) {
    myFixture.checkResult(text)
  }
}
