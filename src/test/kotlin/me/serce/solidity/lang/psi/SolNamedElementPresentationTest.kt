package me.serce.solidity.lang.psi

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.utils.SolTestBase

class SolNamedElementPresentationTest : SolTestBase() {

  fun testFunctionPresentationIncludesContract() {
    val psiFile = InlineFile(
      """
      contract C {
          function foo() {}
      }
      """.trimIndent(),
      "contracts.sol"
    ).psiFile

    val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
      .single { it.name == "foo" }

    val presentation = function.presentation
    val location = presentation?.locationString

    assertNotNull("Expected non-null presentation location for function foo", location)
    assertTrue("Location should include contract name prefix", location!!.startsWith("C - "))

    val path = FileUtil.toSystemIndependentName(location.substringAfter("C - "))
    assertTrue(
      "Location should point to contracts.sol but was $path",
      path.endsWith("contracts.sol")
    )
  }

  fun testFunctionPresentationIncludesContractFromSubdirectory() {
    val psiFile = myFixture.addFileToProject(
      "nested/contracts.sol",
      """
      contract C {
          function foo() {}
      }
      """.trimIndent()
    )

    val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
      .single { it.name == "foo" }

    val presentation = function.presentation
    val location = presentation?.locationString

    assertNotNull("Expected non-null presentation location for function foo", location)
    assertTrue("Location should include contract name prefix", location!!.startsWith("C - "))

    val path = FileUtil.toSystemIndependentName(location.substringAfter("C - "))
    assertTrue(
      "Location should include nested/contracts.sol but was $path",
      path.endsWith("nested/contracts.sol")
    )
  }

  fun testFreeFunctionPresentationShowsFilePathOnly() {
    val psiFile = InlineFile(
      """
      function bar() {}
      """.trimIndent(),
      "free.sol"
    ).psiFile

    val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
      .single { it.name == "bar" }

    val presentation = function.presentation
    val location = presentation?.locationString

    assertNotNull("Expected non-null presentation location for function bar", location)

    val normalized = FileUtil.toSystemIndependentName(location!!)
    assertTrue(
      "Location should point to free.sol but was $normalized",
      normalized.endsWith("free.sol")
    )
    assertFalse("Location should not include contract prefix for free function", location.contains(" - "))
  }
}
