package me.serce.solidity.lang.completion

class SolImportCompletionTest : SolCompletionTestBase() {
    fun testImportCompletionWithEndQuoteAfterCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import "./te/*caret*/"""", """import "./test.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import "./te/*caret*/";""", """import "./test.sol";"""
        )
    }

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/"""", """import {test} from "./test.sol""""
        )
    }

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuoteAndSemicolon() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/";""", """import {test} from "./test.sol";"""
        )
    }
}