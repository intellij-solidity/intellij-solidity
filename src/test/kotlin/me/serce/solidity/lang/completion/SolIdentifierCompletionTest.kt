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
      code = "contract test {}", name = "test.sol"
    )

    checkResultAfterCompletion(
      """contract A is tes/*caret*/{}""", """import {test} from "./test.sol";

contract A is test{}"""
    )
  }

  fun testCompletionWithImportRecursion() {
    InlineFile(
      code = """contract test {}""", name = "test.sol"
    )

    InlineFile(
      code = """import "./rec2.sol"; contract rec1 {}""", name = "rec1.sol"
    )

    InlineFile(
      code = """import "./rec1.sol"; contract rec2 {}""", name = "rec2.sol"
    )

    checkResultAfterCompletion(
      """import "./rec1.sol"; contract A is tes/*caret*/{}""", """import "./rec1.sol";
import {test} from "./test.sol";

contract A is test{}"""
    )
  }
}
