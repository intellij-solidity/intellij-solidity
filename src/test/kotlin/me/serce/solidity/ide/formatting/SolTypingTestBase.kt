package me.serce.solidity.ide.formatting

import me.serce.solidity.utils.SolTestBase

abstract class SolTypingTestBase : SolTestBase() {
  protected fun doTest(c: Char = '\n') = checkByFile {
    myFixture.type(c)
  }
}
