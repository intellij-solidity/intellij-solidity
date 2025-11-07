package me.serce.solidity.lang.completion

class SolImportParentPathCompletionTest : SolCompletionTestBase() {
    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import "../inter/*caret*/";""", """import "../interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile2() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import "../interfaces/IE/*caret*/";""", """import "../interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import "../inter/*caret*/"""", """import "../interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile2() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import "../interfaces/IE/*caret*/"""", """import "../interfaces/IERC20.sol""""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile3() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import {IERC20} from "../inter/*caret*/";""", """import {IERC20} from "../interfaces";"""
        )
    }

    fun testImportCompletionWithEndQuoteAndSemicolonAfterCompletionPreviousFile4() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import {IERC20} from "../interfaces/IE/*caret*/";""",
            """import {IERC20} from "../interfaces/IERC20.sol";"""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile3() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import {IERC20} from "../inter/*caret*/"""", """import {IERC20} from "../interfaces""""
        )
    }

    fun testImportCompletionWithEndQuoteAfterCompletionPreviousFile4() {
        myFixture.configureByFile("../interfaces/IERC20.sol")
        checkResultAfterCompletion(
            """import {IERC20} from "../interfaces/IE/*caret*/"""",
            """import {IERC20} from "../interfaces/IERC20.sol""""
        )
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/lib/"
}