package me.serce.solidity.ide.quickFix

import com.intellij.lang.annotation.HighlightSeverity
import me.serce.solidity.utils.SolTestBase

abstract class SolQuickFixTestBase : SolTestBase() {
  fun testQuickFix(text: String, expected: String) {
    InlineFile(text)
    val errors = myFixture.doHighlighting(HighlightSeverity.WARNING)
    assertEquals(1, errors.size)
    for (quickFixActionRange in errors[0].quickFixActionRanges) {
      quickFixActionRange.first.action.invoke(myFixture.project, myFixture.editor, myFixture.file)
    }
    myFixture.checkResult(expected)
  }
}
