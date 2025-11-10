package me.serce.solidity.lang.completion

class SolImportParentPathCompletionTest : SolCompletionTestBase() {
    fun testImportCompletionPreviousFolderWithEndQuoteAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFolderWithEndQuoteAndSemicolon.sol",
        "useCases/CompleteFolderWithEndQuoteAndSemicolon_after.sol"
    )


    fun testImportCompletionPreviousFileWithEndQuoteAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFileWithEndQuoteAndSemicolon.sol", "useCases/CompleteFileWithEndQuoteAndSemicolon_after.sol"
    )


    fun testImportCompletionPreviousFolderWithEndQuote() = checkCompletionParentDir(
        "useCases/CompleteFolderWithEndQuote.sol", "useCases/CompleteFolderWithEndQuote_after.sol"
    )

    fun testImportCompletionPreviousFileWithEndQuote() = checkCompletionParentDir(
        "useCases/CompleteFileWithEndQuote.sol", "useCases/CompleteFileWithEndQuote_after.sol"
    )

    fun testImportCompletionPreviousFolderOnNamedImportWithEndQuoteAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFolderOnNamedImportWithEndQuoteAndSemicolon.sol",
        "useCases/CompleteFolderOnNamedImportWithEndQuoteAndSemicolon_after.sol"
    )

    fun testImportCompletionPreviousFileOnNamedImportWithEndQuoteAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFileOnNamedImportWithEndQuoteAndSemicolon.sol",
        "useCases/CompleteFileOnNamedImportWithEndQuoteAndSemicolon_after.sol"
    )

    fun testImportCompletionPreviousFolderOnNamedImportWithEndQuote() = checkCompletionParentDir(
        "useCases/CompleteFolderOnNamedImportWithEndQuote.sol",
        "useCases/CompleteFolderOnNamedImportWithEndQuote_after.sol"
    )

    fun testImportCompletionPreviousFileOnNamedImportWithEndQuote() = checkCompletionParentDir(
        "useCases/CompleteFileOnNamedImportWithEndQuote.sol", "useCases/CompleteFileOnNamedImportWithEndQuote_after.sol"
    )

    fun testImportCompletionPreservesMultipleParentSegments() {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile("nested/feature/MultiParentImport.sol")
        myFixture.completeBasic()
        val lookupStrings = myFixture.lookupElementStrings ?: error("No lookup elements for import completion")
        assertTrue(
            lookupStrings.any { it.contains("../../interfacesLocal/IERC20Local.sol") })
    }

    fun checkCompletionParentDir(beforePath: String, afterPath: String) {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        myFixture.configureByFile(beforePath)
        val after = myFixture.configureByFile(afterPath)

        checkResultAfterCompletion(after)
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/upDirImports/"
}
