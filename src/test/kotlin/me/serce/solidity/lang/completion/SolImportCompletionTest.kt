package me.serce.solidity.lang.completion

class SolImportCompletionTest : SolCompletionTestBase() {
    fun testImportCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import "./te/*caret*/"""
        )
    }

    fun testImportCompletionWithEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import "./te/*caret*/""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolon() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import "./te/*caret*/";"""
        )
    }

    fun testImportCompletionWithImportAliasPair() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import {test} from "./te/*caret*/"""
        )
    }

    fun testImportCompletionWithImportAliasPairAndEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import {test} from "./te/*caret*/""""
        )
    }

    fun testImportCompletionWithImportAliasPairAndEndQuoteAndSemicolon() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )
        checkCompletion(
            hashSetOf("./test.sol"), """import {test} from "./te/*caret*/";"""
        )
    }

    fun testImportCompletionAfterCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import "./te/*caret*/""", """import "./test.sol""""
        )
    }

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

    fun testImportCompletionWithImportAliasPairAfterCompletion() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/ """, """import {test} from "./test.sol";"""
        )
    }

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuote() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/" """, """import {test} from "./test.sol""""
        )
    }

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuoteAndSemicolon() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "./te/*caret*/"; """, """import {test} from "./test.sol";"""
        )
    }
}