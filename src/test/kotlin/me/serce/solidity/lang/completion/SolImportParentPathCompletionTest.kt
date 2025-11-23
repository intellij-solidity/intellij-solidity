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

    fun testImportCompletionFileMultiParentImportWithSemicolon() = checkCompletionParentDir(
        "nested/feature/CompleteFileMultiParentImportWithSemicolon.sol",
        "nested/feature/CompleteFileMultiParentImportWithSemicolon_after.sol"
    )

    fun testImportCompletionFileMultiParentImport() = checkCompletionParentDir(
        "nested/feature/CompleteFileMultiParentImport.sol", "nested/feature/CompleteFileMultiParentImport_after.sol"
    )

    fun testImportCompletionFileMultiParentImportOnNamedImport() = checkCompletionParentDir(
        "nested/feature/CompleteFileMultiParentImportOnNamedImport.sol",
        "nested/feature/CompleteFileMultiParentImportOnNamedImport_after.sol"
    )

    fun testImportCompletionFileMultiParentImportOnNamedImportWithSemicolon() = checkCompletionParentDir(
        "nested/feature/CompleteFileMultiParentImportOnNamedImportWithSemicolon.sol",
        "nested/feature/CompleteFileMultiParentImportOnNamedImportWithSemicolon_after.sol"
    )

    fun testImportCompletionFolderMultiParentImportWithSemicolon() = checkCompletionParentDir(
        "nested/feature/CompleteFolderMultiParentImportWithSemicolon.sol",
        "nested/feature/CompleteFolderMultiParentImportWithSemicolon_after.sol"
    )

    fun testImportCompletionFolderMultiParentImport() = checkCompletionParentDir(
        "nested/feature/CompleteFolderMultiParentImport.sol", "nested/feature/CompleteFolderMultiParentImport_after.sol"
    )

    fun testImportCompletionFolderMultiParentImportOnNamedImport() = checkCompletionParentDir(
        "nested/feature/CompleteFolderMultiParentImportOnNamedImport.sol",
        "nested/feature/CompleteFolderMultiParentImportOnNamedImport_after.sol"
    )

    fun testImportCompletionFolderMultiParentImportOnNamedImportWithSemicolon() = checkCompletionParentDir(
        "nested/feature/CompleteFolderMultiParentImportOnNamedImportWithSemicolon.sol",
        "nested/feature/CompleteFolderMultiParentImportOnNamedImportWithSemicolon_after.sol"
    )

    fun checkCompletionParentDir(beforePath: String, afterPath: String) {
        myFixture.configureByFile("interfacesLocal/IERC20Local.sol")
        val after = myFixture.configureByFile(afterPath)
        myFixture.configureByFile(beforePath)

        checkResultAfterCompletion(after)
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importCompletion/upDirImports/"
}
