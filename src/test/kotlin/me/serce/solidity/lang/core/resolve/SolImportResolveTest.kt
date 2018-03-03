package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveTest : SolResolveTestBase() {
  fun testImportPathResolve() {
    val file1 = InlineFile(
      name = "Ownable.sol",
      code = "contract a {}"
    )

    InlineFile("""
          import "./Ownable.sol";
                      //^

          contract b {}
    """)

    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals(file1.name, resolved.containingFile.name)
  }

  fun testImportPathResolveNpm() {
    val file1 = myFixture.configureByFile("node_modules/util/contracts/TestImport.sol")
    myFixture.configureByFile("contracts/ImportUsage.sol")

    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals(file1.name, resolved.containingFile.name)
  }

  fun testResolveFrom() {
    val file1 = InlineFile(
      name = "Ownable.sol",
      code = "contract A {}"
    )

    InlineFile("""
          import A from "./Ownable.sol";
               //^

          contract b {}
    """)

    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals(file1.name, resolved.containingFile.name)
  }

  override fun getTestDataPath() = "src/test/resources/fixtures/import/"
}
