package me.serce.solidity.lang.core.resolve

import com.intellij.psi.PsiNamedElement
import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveTest : SolResolveTestBase() {
  fun testImportPathResolve() {
    val file1 = InlineFile(
      code = "contract a {}",
      name = "Ownable.sol"
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

  fun testImportPathResolveEthPM() {
    val file1 = myFixture.configureByFile("installed_contracts/util/contracts/TestImport.sol")
    myFixture.configureByFile("contracts/ImportUsageEthPM.sol")

    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals(file1.name, resolved.containingFile.name)
  }

  fun testImportPathResolveFoundry() {
    val file1 = myFixture.configureByFile("lib/util/src/TestImport.sol")
    myFixture.configureByFile("contracts/ImportUsageFoundry.sol")

    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals(file1.name, resolved.containingFile.name)
  }

  fun testResolveNameClash() {

    myFixture.configureByFile("contracts/a/SimpleName.sol")
    myFixture.configureByFile("contracts/b/SimpleName.sol")

    myFixture.configureByFile("contracts/ImportNameClash.sol")
    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals("abc", resolved.name)
  }

  fun testRecursiveImport() {
    myFixture.configureByFile("recursive/B.sol")
    myFixture.configureByFile("recursive/C.sol")
    myFixture.configureByFile("recursive/dir/C.sol")
    myFixture.configureByFile("recursive/A.sol")
    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals("C", resolved.name)
  }

  override fun getTestDataPath() = "src/test/resources/fixtures/import/"
}
