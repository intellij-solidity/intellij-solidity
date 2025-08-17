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
    """
    ).psiFile
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

  fun testCircularImport() {
    myFixture.configureByFile("circular/IMain.sol")
    myFixture.configureByFile("circular/IRandomLib.sol")
    myFixture.configureByFile("circular/Main.sol")
    val (refElement) = findElementAndDataInEditor<SolNamedElement>("^")

    val resolved = checkNotNull(refElement.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${refElement.text}"
    }

    assertEquals("RandomStruct", resolved.name)
  }

  fun testImportStructAndConstant() {
    InlineFile(
      """
      struct Struct1 {
          string foo;
      }

      string constant STR_CONST = "1";
      
      type UserDefinedType is string;
    """.trimIndent(), "Utils.sol"
    )
    InlineFile(
      """
        pragma solidity ^0.8.0;

        import {Struct1, STR_CONST, UserDefinedType} from "./Utils.sol";

        contract C1 {
            Struct1 public s1;
            //^
            function test() public pure returns (UserDefinedType memory) {
                                                    //^
                return STR_CONST;
                       //^
            }
        }
      """.trimIndent()
    )

    val (structElement, userDefinedTypeElement, constElement) = findMultipleElementAndDataInEditor<SolNamedElement>("^")
    val structRef = structElement.first
    val structResolved = checkNotNull(structRef.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${structRef.text}"
    }
    assertEquals("Struct1", structResolved.name)

    val userDefinedTypeRef = userDefinedTypeElement.first
    val userDefinedTypeResolved = checkNotNull(userDefinedTypeRef.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${userDefinedTypeRef.text}"
    }
    assertEquals("UserDefinedType", userDefinedTypeResolved.name)

    val constRef = constElement.first
    val constResolved = checkNotNull(constRef.reference?.resolve() as? PsiNamedElement) {
      "Failed to resolve ${constRef.text}"
    }
    assertEquals("STR_CONST", constResolved.name)
  }

  override fun getTestDataPath() = "src/test/resources/fixtures/import/"
}
