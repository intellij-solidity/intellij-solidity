package me.serce.solidity.lang.completion

import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolCompletionTestBase : SolTestBase() {
  protected fun checkCompletion(required: Set<String>, @Language("Solidity") code: String, strict: Boolean = false) {
    InlineFile(code).withCaret()
    val variants = myFixture.completeBasic()
    checkNotNull(variants) {
      "Expected completions that contain $required, but no completions found"
    }
    if (strict) {
      assertEquals(required.toHashSet(), variants.map { it.lookupString }.toHashSet())
    } else {
      assertTrue(variants.map { it.lookupString }.toHashSet().containsAll(required))
    }
  }
}
