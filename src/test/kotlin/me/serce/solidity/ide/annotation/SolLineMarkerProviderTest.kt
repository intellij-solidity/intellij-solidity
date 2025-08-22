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
}
