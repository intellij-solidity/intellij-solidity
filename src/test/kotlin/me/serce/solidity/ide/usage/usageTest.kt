package me.serce.solidity.ide.usage

import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolUsageTestBase : SolTestBase() {
  protected fun doTest(@Language("Solidity") code: String, expectedUsages: Int) {
    InlineFile(code)
    val source = findElementInEditor<SolNamedElement>()
    val usages = myFixture.findUsages(source)
    assertEquals(expectedUsages, usages.size)
  }

  protected fun multipleUsageTest(@Language("Solidity") code: String) {
    InlineFile(code)
    val source = findElementInEditor<SolNamedElement>()
    val usages = myFixture.findUsages(source)
    val elements = findMultipleElementAndDataInEditor<SolNamedElement>().map { it.first }
    assertTrue("Failed to find all usages ", usages.all { elements.contains(it.element) })
  }
}
