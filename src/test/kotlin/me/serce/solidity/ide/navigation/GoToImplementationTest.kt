package me.serce.solidity.ide.navigation

import com.intellij.codeInsight.navigation.GotoImplementationHandler
import com.intellij.codeInsight.navigation.GotoTargetHandler.GotoData
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language
import org.junit.Assert

class GoToImplementationTest : SolTestBase() {

  fun testFindImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
  """, setOf("A", "B"))

  fun testFindMultipleImplementations() = testImplementations("""
      contract A/*caret*/ { }
      contract B is A { }
      contract C is B { }
  """, setOf("B", "C"))

  private fun testImplementations(@Language("Solidity") code: String, options: Set<String>) {
    InlineFile(code).withCaret()
    val handler = GotoImplementationHandler()

    val data: GotoData? = handler.getSourceAndTargetElements(myFixture.editor, myFixture.file)
    if(data == null) {
      throw RuntimeException("Can't find implementations")
    }
    Assert.assertEquals(options, data.targets.map { (it as SolContractDefinition).name }.toSet())
  }
}
