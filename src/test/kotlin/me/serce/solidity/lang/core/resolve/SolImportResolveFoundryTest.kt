package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveFoundryTest : SolResolveTestBase() {

    private val testcases = arrayListOf(
        Pair("lib/forge-std/src/Test.sol", "contracts/ImportUsageFoundryStd.sol"),
        Pair("lib/solmate/src/tokens/ERC721.sol", "contracts/ImportUsageFoundrySolmate.sol"),
        Pair(
            "lib/openzeppelin-contracts/contracts/token/ERC20/ERC20.sol", "contracts/ImportUsageFoundryOpenzeppelin.sol"
        ),
    );

    fun testImportPathResolveFoundryRemappings() {
        testImportPathResolveWithConfig("remappings.txt")
    }

    fun testImportPathResolveFoundryFoundryFile() {
        testImportPathResolveWithConfig("foundry.toml")
    }

    fun testImportPathResolveFoundryFoundryFileNoError() {
        testcases.forEach { (_, contractFile) ->
            myFixture.configureByText("foundry.toml", "")
            myFixture.configureByFile(contractFile)
            val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
            check(refElement.reference?.resolve() == null) {
                "Should failed to resolve ${refElement.text}"
            }
        }
    }

    override fun getTestDataPath() = "src/test/resources/fixtures/importRemappings/"

    private fun testImportPathResolveWithConfig(configFileName: String) {
        testcases.forEach { (targetFile, contractFile) ->
            val expectedFile = myFixture.configureByFile(targetFile)
            myFixture.configureByFile(configFileName)
            myFixture.configureByFile(contractFile)

            val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
            val resolved = checkNotNull(refElement.reference?.resolve()) {
                "Failed to resolve ${refElement.text}"
            }

            assertEquals(expectedFile.name, resolved.containingFile.name)
        }
    }
}
