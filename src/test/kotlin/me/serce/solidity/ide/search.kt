package me.serce.solidity.ide

import junit.framework.Assert
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolFindUsagesTest : SolTestBase() {
  fun testFindInheritance() = doTest("""
      contract A {}
             //^
      contract B is A {}
      contract C is A {}
  """, 2)

  fun testFields() = doTest("""
      contract A {}
             //^
      contract B {
          A field1;
          A field2;
      }
      contract C {
          A field1;
      }
    """, 3)

  private fun doTest(@Language("Solidity") code: String, expectedUsages: Int) {
    InlineFile(code)
    val source = findElementInEditor<SolNamedElement>()
    val usages = myFixture.findUsages(source)
    Assert.assertEquals(expectedUsages, usages.size)
  }
}
