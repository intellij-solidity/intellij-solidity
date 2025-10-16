package me.serce.solidity.lang.completion

class SolImportCompletionTest : SolCompletionTestBase() {
    fun testImportCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/ """, """import {test} from "./test.sol";"""
        )
    }

    fun testImportCompletionWithAlreadyEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/" """, """import {test} from "./test.sol";"""
        )
    }
}