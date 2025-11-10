package me.serce.solidity.lang.completion

class SolImportParentPathCompletionTest : SolCompletionTestBase() {
    fun testImportCompletionPreviousFolderAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFolderAndSemicolon.sol",
        "useCases/CompleteFolderAndSemicolon_after.sol"
    )


    fun testImportCompletionPreviousFileAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFileAndSemicolon.sol", "useCases/CompleteFileAndSemicolon_after.sol"
    )


    fun testImportCompletionPreviousFolder() = checkCompletionParentDir(
        "useCases/CompleteFolder.sol", "useCases/CompleteFolder_after.sol"
    )

    fun testImportCompletionPreviousFile() = checkCompletionParentDir(
        "useCases/CompleteFile.sol", "useCases/CompleteFile_after.sol"
    )

    fun testImportCompletionPreviousFolderOnNamedImportAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFolderOnNamedImportAndSemicolon.sol",
        "useCases/CompleteFolderOnNamedImportAndSemicolon_after.sol"
    )

    fun testImportCompletionPreviousFileOnNamedImportAndSemicolon() = checkCompletionParentDir(
        "useCases/CompleteFileOnNamedImportAndSemicolon.sol",
        "useCases/CompleteFileOnNamedImportAndSemicolon_after.sol"
    )

    fun testImportCompletionPreviousFolderOnNamedImport() = checkCompletionParentDir(
        "useCases/CompleteFolderOnNamedImport.sol",
        "useCases/CompleteFolderOnNamedImport_after.sol"
    )

    fun testImportCompletionPreviousFileOnNamedImport() = checkCompletionParentDir(
        "useCases/CompleteFileOnNamedImport.sol", "useCases/CompleteFileOnNamedImport_after.sol"
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
