package me.serce.solidity.ide.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language
import org.junit.Assert
import java.util.concurrent.Callable

class GoToImplementationTest : SolTestBase() {

  fun testFindImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
  """, setOf("B"))

  fun testFindMultipleImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
      contract C is B { }
  """, setOf("B", "C"))

  private fun testImplementations(@Language("Solidity") code: String, options: Set<String>) {
    InlineFile(code).withCaret()
    val actual = doGoToImplementation()
    Assert.assertEquals(options, actual)
  }

  // Thank you, IntelliJ Rust authors who spared me from writing this code, as I was able to copy it from
  // https://github.com/intellij-rust/intellij-rust/blob/a0ab79286ab29e723c2d890ee123dacb43560184/src/test/kotlin/org/rust/ide/navigation/goto/RsGotoImplementationsTest.kt#L220-L232
  // Forever grateful.
  private fun doGoToImplementation(): Set<String> {
    val data = CodeInsightTestUtil.gotoImplementation(myFixture.editor, myFixture.file)

    val future = ApplicationManager.getApplication().executeOnPooledThread(Callable {
      runReadAction {
        @Suppress("UnstableApiUsage") data.targets.map { GotoTargetHandler.computePresentation(it, data.hasDifferentNames()) }
          // Copied from `com.intellij.codeInsight.navigation.GotoTargetHandler.GotoData.getComparingObject`
          .map { listOfNotNull(it.presentableText, it.containerText, it.locationText).joinToString(" ") }
      }
    })
    return ProgressIndicatorUtils.awaitWithCheckCanceled(future).map { it.trim() }.toSet()
  }
}
