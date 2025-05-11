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
    val completions = variants.map { it.lookupString }.toHashSet()
    if (strict) {
      assertEquals(required.toHashSet(), completions)
    } else {
      assertTrue("$completions doesn't contain all $required", completions.containsAll(required))
    }
  }

  val elementaryTypeB = hashSetOf(
    "bytes ",
    "bytes4 ",
    "bytes8 ",
    "bytes16 ",
    "bytes20 ",
    "bytes32 ",
    "byte ",
    "bool "
  )

  val elementaryType = hashSetOf(
    "address ",
    "string ",
    "fixed",
    "ufixed",
    "uint8 ",
    "uint16 ",
    "uint32 ",
    "uint64 ",
    "uint128 ",
    "uint256 ",
    "int8 ",
    "int16 ",
    "int32 ",
    "int64 ",
    "int128 ",
    "int256 "
  ) + elementaryTypeB

  val dataLocation = hashSetOf(
    "memory",
    "storage",
    "calldata"
  )

  val stateMutability = hashSetOf(
    "pure", "view", "payable"
  )

  val contractBodyElement = hashSetOf(
    "constructor",
    "function ",
    "modifier ",
    "fallback",
    "receive",
    "struct ",
    "enum ",
    "type ",
    "event ",
    "error ",
    "using ",
    "mapping",
    "this"
  ) + elementaryType
}
