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

    fun testImportCompletionWithEndQuoteAfterCompletion2() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import "/te/*caret*/"""", """import "/test.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletion2() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import "/te/*caret*/";""", """import "/test.sol";"""
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

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuote2() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "/te/*caret*/"""", """import {test} from "/test.sol""""
        )
    }

    fun testImportCompletionWithImportAliasPairAfterCompletionWithEndQuoteAndSemicolon2() {
        InlineFile(
            code = """contract test {}""", name = "test.sol"
        )

        checkResultAfterCompletion(
            """import {test} from "/te/*caret*/";""", """import {test} from "/test.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionSubDirectory() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/"""", """import "./interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionSubDirectory() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/";""", """import "./interfaces";"""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAfterCompletionSubDirectory() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import {test} from "./interf/*caret*/"""", """import {test} from "./interfaces""""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAndSemicolonAfterCompletionSubDirectory() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import {test} from "./interf/*caret*/";""", """import {test} from "./interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionSubFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import "./interfaces/IE/*caret*/"""", """import "./interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionSubFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import "./interfaces/IE/*caret*/";""", """import "./interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAfterCompletionSubFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "./interfaces/IE/*caret*/"""", """import {IERC20} from "./interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAndSemicolonAfterCompletionSubFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "./interfaces/IE/*caret*/";""",
            """import {IERC20} from "./interfaces/IERC20.sol";"""
        )
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/"
}