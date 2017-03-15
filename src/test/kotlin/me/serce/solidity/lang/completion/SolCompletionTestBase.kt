package me.serce.solidity.lang.completion

import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolCompletionTestBase : SolTestBase() {
  protected fun checkCompletion(target: Set<String>, @Language("Solidity") code: String) {
    InlineFile(code).withCaret()
    val variants = myFixture.completeBasic()
    checkNotNull(variants) {
      "Expected completions that contain $target, but no completions found"
    }
    assertEquals(target, variants.map { it.lookupString }.toHashSet())
  }
}
