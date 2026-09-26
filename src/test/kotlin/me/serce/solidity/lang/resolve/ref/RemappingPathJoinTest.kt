package me.serce.solidity.lang.resolve.ref

import junit.framework.TestCase

class RemappingPathJoinTest : TestCase() {
  fun testTargetWithoutTrailingSlashIsSeparated() {
    assertEquals(
      "node_modules/@chainlink/contracts/src/Token.sol",
      joinRemappingTarget("node_modules/@chainlink/contracts", "src/Token.sol")
    )
  }

  fun testDoubleSlashIsCollapsed() {
    assertEquals(
      "lib/foo/src/Bar.sol",
      joinRemappingTarget("lib/foo/src/", "/Bar.sol")
    )
  }

  fun testTargetWithSlashAndRestWithout() {
    assertEquals(
      "lib/foo/src/Bar.sol",
      joinRemappingTarget("lib/foo/src/", "Bar.sol")
    )
  }

  fun testTargetWithoutSlashAndRestWithSlash() {
    assertEquals(
      "lib/foo/src/Bar.sol",
      joinRemappingTarget("lib/foo/src", "/Bar.sol")
    )
  }

  fun testEmptyRestKeepsTarget() {
    assertEquals("lib/foo/src/", joinRemappingTarget("lib/foo/src/", ""))
  }
}
