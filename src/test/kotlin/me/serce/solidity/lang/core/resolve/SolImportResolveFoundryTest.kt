package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveFoundryTest : SolResolveTestBase() {

  private val testcases = arrayListOf(
    Pair("lib/forge-std/src/Test.sol","contracts/ImportUsageFoundryStd.sol"),
    Pair("lib/solmate/src/tokens/ERC721.sol","contracts/ImportUsageFoundrySolmate.sol"),
    Pair("lib/openzeppelin-contracts/contracts/token/ERC20/ERC20.sol","contracts/ImportUsageFoundryOpenzeppelin.sol"),
  );

  fun testImportPathResolveFoundryRemappings() {
    testcases.forEach { (targetFile, contractFile) ->
      val file1 = myFixture.configureByFile(targetFile)
      myFixture.configureByFile("remappings.txt")
      myFixture.configureByFile(contractFile)
      val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
      val resolved = checkNotNull(refElement.reference?.resolve()) {
        "Failed to resolve ${refElement.text}"
      }
      assertEquals(file1.name, resolved.containingFile.name)
    }
  }

  fun testImportPathResolveFoundryFoundryFile() {
    testcases.forEach { (targetFile, contractFile) ->
      val file1 = myFixture.configureByFile(targetFile)
      myFixture.configureByFile("foundry.toml")
      myFixture.configureByFile(contractFile)
      val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
      val resolved = checkNotNull(refElement.reference?.resolve()) {
        "Failed to resolve ${refElement.text}"
      }
      assertEquals(file1.name, resolved.containingFile.name)
    }
  }

  fun testImportPathResolveFoundryFoundryFileNoError() {
    testcases.forEach { (targetFile, contractFile) ->
      val file1 = myFixture.configureByFile(targetFile)
      myFixture.configureByFile("foundry.toml")
      myFixture.configureByFile(contractFile)
      val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")
      val resolved = checkNotNull(refElement.reference?.resolve()) {
        "Failed to resolve ${refElement.text}"
      }
      assertEquals(file1.name, resolved.containingFile.name)
    }
  }

  override fun getTestDataPath() = "src/test/resources/fixtures/importRemappings/"
}
