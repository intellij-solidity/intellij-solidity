package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.impl.merge
import me.serce.solidity.lang.types.SolContract

class SolC3LinearizationTest : SolResolveTestBase() {

  fun testMerge() {
    assertEquals("DFO".toList(), listOf("DO".toList(), "FO".toList(), "DF".toList()).merge())
  }

  fun testMergeImpossible() {
    try {
      listOf("CAB".toList(), "DBA".toList(), "CD".toList()).merge()
      fail("should fail")
    } catch (e: IllegalStateException) {
      assertEquals("Unable to merge lists: result: [C, D] lists: [[A, B], [B, A]]", e.message)
    }
  }

  fun testOne() {
    val (code, _) = resolveInCode<SolContractDefinition>(
      """
        contract A {}
               //^
          """
    )
    assertEquals(SolContract(code).linearize().size, 1)
  }

  fun testSimple() {
    val (code, _) = resolveInCode<SolContractDefinition>(
      """
        contract Base {}
        contract A is Base {}
               //^
          """
    )
    assertEquals(SolContract(code).linearize().size, 2)
  }

  fun testFromExamples() {
    val (code, _) = resolveInCode<SolContractDefinition>(
      """
        contract O {}
        contract A is O {}
        contract B is O {}
        contract C is O {}
        contract D is O {}
        contract E is O {}
        contract K1 is A, B, C {}
        contract K2 is D, B, E {}
        contract K3 is D, A {}
        contract Z is K1, K2, K3 {}
               //^
          """
    )
    assertEquals(SolContract(code).linearize().map { it.toString() }, listOf("Z", "K1", "K2", "K3", "D", "A", "B", "C", "E", "O"))
  }
}
