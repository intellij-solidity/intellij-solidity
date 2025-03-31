package me.serce.solidity.lang.core.resolve

import com.intellij.psi.PsiNamedElement
import me.serce.solidity.lang.psi.SolNamedElement

class SolImportResolveTest : SolResolveTestBase() {
  fun testImportPathResolve() = testResolveToAnotherFile(
    InlineFile(
      code = "contract a {}",
      name = "Ownable.sol"
    ).psiFile,
    InlineFile(
      """
          import "./Ownable.sol";
                      //^

          contract b {}
    """).psiFile
  )

  fun testImportPathResolveNpm() = testResolveToAnotherFile(
    myFixture.configureByFile("node_modules/util/contracts/TestImport.sol"),
    myFixture.configureByFile("contracts/ImportUsage.sol")
  )

  fun testImportPathResolveEthPM() = testResolveToAnotherFile(
    myFixture.configureByFile("installed_contracts/util/contracts/TestImport.sol"),
    myFixture.configureByFile("contracts/ImportUsageEthPM.sol")
  )


  fun testImportPathResolveFoundry() = testResolveToAnotherFile(
    myFixture.configureByFile("lib/util/src/TestImport.sol"),
    myFixture.configureByFile("contracts/ImportUsageFoundry.sol")
  )


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

  fun testResolveFunctionImportClash() {
    myFixture.configureByFile("contracts/a/lib.sol")
    myFixture.configureByFile("contracts/b/b.sol")
    myFixture.configureByFile("contracts/b/lib.sol")

    myFixture.configureByFile("contracts/ImportLibNameClash.sol")
    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals("lib", resolved.name)
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
