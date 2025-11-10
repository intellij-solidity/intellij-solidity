package me.serce.solidity.lang.completion

class SolImportParentPathCompletionTest : SolCompletionTestBase() {
    fun testImportCompletionPreviousFolderWithEndQuoteAndSemicolon() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("useCases/CompleteFolderWithEndQuoteAndSemicolon.sol")
        val after = myFixture.configureByFile("useCases/CompleteFolderWithEndQuoteAndSemicolon_after.sol")

        checkResultAfterCompletion(after)
    }

    fun testImportCompletionPreviousFileWithEndQuoteAndSemicolon() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("useCases/CompleteFileWithEndQuoteAndSemicolon.sol")
        val after = myFixture.configureByFile("useCases/CompleteFileWithEndQuoteAndSemicolon_after.sol")

        checkResultAfterCompletion(after)
    }

    fun testImportCompletionPreviousFolderWithEndQuote() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("useCases/CompleteFolderWithEndQuote.sol")
        val after = myFixture.configureByFile("useCases/CompleteFolderWithEndQuote_after.sol")

        checkResultAfterCompletion(after)
    }

    fun testImportCompletionPreviousFileWithEndQuote() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("useCases/CompleteFileWithEndQuote.sol")
        val after = myFixture.configureByFile("useCases/CompleteFileWithEndQuote_after.sol")

        checkResultAfterCompletion(after)
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

    fun testImportCompletionPreservesMultipleParentSegments() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("nested/feature/MultiParentImport.sol")
        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings
            ?: error("No lookup elements for import completion")
        assertTrue(
            lookupStrings.any { it.contains("../../interfacesLocal/IERC20Local.sol") }
        )
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/upDirImports/"
}
