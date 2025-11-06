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
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/"""", """import "./interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionSubDirectory() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interf/*caret*/";""", """import "./interfaces";"""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAfterCompletionSubDirectory() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import {test} from "./interf/*caret*/"""", """import {test} from "./interfaces""""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAndSemicolonAfterCompletionSubDirectory() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import {test} from "./interf/*caret*/";""", """import {test} from "./interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionSubFile() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interfaces/IE/*caret*/"""", """import "./interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionSubFile() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import "./interfaces/IE/*caret*/";""", """import "./interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAfterCompletionSubFile() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "./interfaces/IE/*caret*/"""", """import {IERC20} from "./interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionImportAliasWithEndQuoteAndSemicolonAfterCompletionSubFile() {
        myFixture.configureByFile("lib/lib.sol")
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.configureByFile("src/ERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "./interfaces/IE/*caret*/";""", """import {IERC20} from "./interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import "../inter/*caret*/";""", """import "../interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile2() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import "../interfaces/IE/*caret*/";""", """import "../interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import "../inter/*caret*/"""", """import "../interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile2() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import "../interfaces/IE/*caret*/"""", """import "../interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile3() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "../inter/*caret*/";""", """import {IERC20} from "../interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile4() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "../interfaces/IE/*caret*/";""", """import {IERC20} from "../interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile3() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "../inter/*caret*/"""", """import {IERC20} from "../interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile4() {
        myFixture.configureByFile("interfaces/IERC20.sol")
        myFixture.copyFileToProject("interfaces/IERC20.sol", "src/test/resources/fixtures/IERC20.sol")

        checkResultAfterCompletion(
            """import {IERC20} from "../interfaces/IE/*caret*/"""", """import {IERC20} from "../interfaces/IERC20.sol""""
        )
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/"
}