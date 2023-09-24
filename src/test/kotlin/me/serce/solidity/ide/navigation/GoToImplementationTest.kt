package me.serce.solidity.ide.navigation

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language
import org.junit.Assert
import java.util.concurrent.Callable

class GoToImplementationTest : SolTestBase() {

  fun testFindImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
  """, setOf("B"), "ctr.sol")

  fun testFindMultipleImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
      contract C is B { }
  """, setOf("B", "C"), "ctr.sol")

  fun testImportContract() {
    InlineFile(
      code = """
          import {Ctr} from './ctr.sol';
          contract Ctr1 is Ctr {
          }
      """,
      name = "base1.sol"
    )

    testImplementations(
      """
        contract Ctr/*caret*/ {
        }
      """.trimIndent(), setOf("Ctr1"), "ctr.sol")
  }

  fun testImportContractAs() {
    InlineFile(
      code = """
          import {Ctr as MyCtr} from './ctr2.sol';
          contract Ctr1 is MyCtr {
          }
      """,
      name = "base1.sol"
    )

    testImplementations(
      """
        contract Ctr/*caret*/ {
        }
      """.trimIndent(), setOf("Ctr1"), "ctr2.sol")
  }



  private fun testImplementations(@Language("Solidity") code: String, options: Set<String>, filename: String) {
    InlineFile(code, name=filename).withCaret()
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
