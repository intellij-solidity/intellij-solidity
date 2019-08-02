package me.serce.solidity.lang.types

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolTypeTest {
  @Test
  fun testUintIntCanBeApplied() {
    val uint256 = SolInteger(true, 256)
    val uint248 = SolInteger(true, 248)
    val int256 = SolInteger(false, 256)

    assertFalse(uint256.isAssignableFrom(int256))
    assertFalse(int256.isAssignableFrom(uint256))
    assertTrue(int256.isAssignableFrom(uint248))
  }
}
