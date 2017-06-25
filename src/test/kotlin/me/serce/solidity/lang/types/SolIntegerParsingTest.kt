package me.serce.solidity.lang.types

import org.junit.Test

import org.junit.Assert.*

class SolIntegerParsingTest {
  @Test
  fun parseInt() {
    assertEquals(SolInteger(false, 256), SolInteger.parse("int"))
  }

  @Test
  fun parseInt8() {
    assertEquals(SolInteger(false, 8), SolInteger.parse("int8"))
  }

  @Test
  fun parseUInt() {
    assertEquals(SolInteger(true, 256), SolInteger.parse("uint"))
  }

  @Test
  fun parseUInt128() {
    assertEquals(SolInteger(true, 128), SolInteger.parse("uint128"))
  }
}
