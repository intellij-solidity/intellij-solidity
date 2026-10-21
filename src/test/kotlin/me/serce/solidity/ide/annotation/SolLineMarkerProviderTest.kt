package me.serce.solidity.ide.annotation

import com.intellij.icons.AllIcons
import me.serce.solidity.utils.SolTestBase

class SolLineMarkerProviderTest : SolTestBase() {
  fun testFunctionGutterIcons() {
    InlineFile(
      """
          contract A {
              function foo() public {}
          }

          contract B is A {
              function foo() public override {}
          }
          /*caret*/
      """,
      name = "A.sol"
    ).withCaret()

    val gutters = myFixture.findAllGutters()

    val overridingGutter = gutters.find { it.tooltipText == "Overrides function" }
    assertNotNull(overridingGutter)
    assertEquals(AllIcons.Gutter.OverridingMethod, overridingGutter?.icon)

    val overriddenGutter = gutters.find { it.tooltipText == "Is overridden in subcontracts" }
    assertNotNull(overriddenGutter)
    assertEquals(AllIcons.Gutter.OverridenMethod, overriddenGutter?.icon)
  }

  fun testNoGutterIconsForNonOverriddenFunction() {
    InlineFile(
      """
          contract A {
              function foo() public {}
          }
          /*caret*/
      """,
      name = "A.sol"
    ).withCaret()

    val gutters = myFixture.findAllGutters()
    val functionGutters = gutters.filter {
      it.tooltipText == "Overrides function" ||
        it.tooltipText == "Is overridden in subcontracts"
    }
    assertTrue(functionGutters.isEmpty())
  }

  fun testOverriddenGutterForDeeplyInheritedOverride() {
    InlineFile(
      """
          contract A {
              function foo() public {}
          }

          contract B is A {
          }

          contract C is B {
              function foo() public override {}
          }
          /*caret*/
      """,
      name = "A.sol"
    ).withCaret()

    val gutters = myFixture.findAllGutters()
    val overriddenGutter = gutters.find { it.tooltipText == "Is overridden in subcontracts" }
    assertNotNull(overriddenGutter)
    assertEquals(AllIcons.Gutter.OverridenMethod, overriddenGutter?.icon)
  }

  fun testContractGutterForDeeplyInheritedImplementation() {
    InlineFile(
      """
          contract A {
          }

          contract B is A {
          }

          contract C is B {
          }
          /*caret*/
      """,
      name = "A.sol"
    ).withCaret()

    val gutters = myFixture.findAllGutters()
    val implementationGutter = gutters.find { it.tooltipText == "Has implementations" }
    assertNotNull(implementationGutter)
    assertEquals(AllIcons.Gutter.OverridenMethod, implementationGutter?.icon)
  }
}
