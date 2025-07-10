package me.serce.solidity.ide.run

import com.intellij.icons.AllIcons
import me.serce.solidity.utils.SolTestBase

class ForgeTestLineMarkerProviderTest : SolTestBase() {
  fun testGutterIconsForTestFile() {
    InlineFile("""
        //- Counter.t.sol
        import "forge-std/Test.sol";

        contract CounterTest is Test {
            function testIncrement() public {
                assertTrue(true);
            }

            function testDecrement() public {
                // some test logic
            }

            function helperFunction() internal {
                // not a test
            }
        }/*caret*/
    """, name = "Counter.t.sol").withCaret()

    val gutters = myFixture.findAllGutters()
    assertEquals(3, gutters.size) // 1 for contract, 2 for test functions

    val contractGutter = gutters.find { it.tooltipText == "Run Forge Tests in CounterTest" }
    assertNotNull(contractGutter)
    assertEquals(AllIcons.RunConfigurations.TestState.Run, contractGutter?.icon)

    val testIncrementGutter = gutters.find { it.tooltipText == "Run Forge Test testIncrement" }
    assertNotNull(testIncrementGutter)

    val testDecrementGutter = gutters.find { it.tooltipText == "Run Forge Test testDecrement" }
    assertNotNull(testDecrementGutter)
  }

  fun testNoGutterIconsForNonTestFile() {
    InlineFile("""
        //- Counter.sol
        contract Counter {
            uint256 public number;

            function setNumber(uint256 newNumber) public {
                number = newNumber;
            }

            function increment() public {
                number++;
            }
        }/*caret*/
    """, name = "Counter.sol").withCaret()

    val gutters = myFixture.findAllGutters()
    assertTrue(gutters.isEmpty())
  }

  fun testGutterIconsForPublicAndExternalFunctionsOnly() {
    InlineFile("""
        //- Visibility.t.sol
        import "forge-std/Test.sol";

        contract VisibilityTest is Test {
            function testPublic() public {}
            function testExternal() external {}
            function testInternal() internal {}
            function testPrivate() private {}
            function testNoVisibility() {} // defaults to public
        }/*caret*/
    """, name = "Visibility.t.sol").withCaret()

    val gutters = myFixture.findAllGutters()
      .filter { it.tooltipText?.startsWith("Run Forge Test") == true && it.tooltipText?.startsWith("Run Forge Tests in") != true }
      .map { it.tooltipText }

    // Check that we have gutter icons for the expected functions
    assertTrue(gutters.contains("Run Forge Test testPublic"))
    assertTrue(gutters.contains("Run Forge Test testExternal"))
    assertTrue(gutters.contains("Run Forge Test testNoVisibility"))
    assertEquals(3, gutters.size)
  }
}
