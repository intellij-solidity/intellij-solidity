package me.serce.solidity.ide.quickFix

import com.intellij.lang.annotation.HighlightSeverity
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolQuickFixTestBase : SolTestBase() {
  fun checkQuickFix(@Language("Solidity") code: String, expected: String) {
    InlineFile(code)
    val errors = myFixture.doHighlighting(HighlightSeverity.WARNING)
    assertEquals(1, errors.size)
    for (quickFixActionRange in errors[0].quickFixActionRanges!!) {
      quickFixActionRange.first.action.invoke(myFixture.project, myFixture.editor, myFixture.file)
    }
    myFixture.checkResult(expected)
  }

  fun assertNoQuickFix(@Language("Solidity") code: String) {
    InlineFile(code)
    val errors = myFixture.doHighlighting(HighlightSeverity.WARNING)
    assertEquals(0, errors.size)
  }
}
