package me.serce.solidity.lang.completion

class SolImportCompletionTest : SolCompletionTestBase() {
    fun testImportCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """
        import {test} from "./te/*caret*/
  """
        )
    }

    fun testImportCompletionWithEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("\"./test.sol\""), """
        import {test} from "./te/*caret*/"
  """
        )
    }

    fun testImportCompletionAfterCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/ """, """import {test} from "./test.sol";"""
        )
    }

    fun testImportCompletionAfterCompletionWithEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/" """, """import {test} from "./test.sol";"""
        )
    }
}