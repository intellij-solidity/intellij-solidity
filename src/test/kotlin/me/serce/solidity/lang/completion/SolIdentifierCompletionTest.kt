package me.serce.solidity.lang.completion

class SolIdentifierCompletionTest : SolCompletionTestBase() {
  fun testVariableCompletion() = checkCompletion(hashSetOf("Foo", "FooBar"), """
    contract Foo {}
    contract FooBar {}

    contract A {
        function f() {
            Fo/*caret*/ kk;
        }
    }

  """)

  fun testCompletionWithImport() {
    InlineFile(
      code = "contract test {}",
      name = "test.sol"
    )

    InlineFile("""

    contract A is tes/*caret*/{}""").withCaret()
    myFixture.completeBasic()
    myFixture.checkResult("""import "./test.sol";

contract A is test{}""")
  }
}
