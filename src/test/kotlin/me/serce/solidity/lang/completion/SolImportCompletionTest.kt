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

    fun testImportCompletionWithEndQuoteAfterCompletionSubFiles() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/"""", """import "./interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionSubFiles() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/";""", """import "./interfaces";"""
        )
    }
override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/"
}