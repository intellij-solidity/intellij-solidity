package me.serce.solidity.lang.psi

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolContractCollectSupersTest : SolResolveTestBase() {
  fun testCollect() {
    val (c, _) = resolveInCode<SolContractDefinition>("""
        contract A {}
               //x

        contract B is A {}

        contract C is B {}
               //^
  """)

    assertEquals(listOf("B", "A"), c.collectSupers.map { it.name })
  }
}
