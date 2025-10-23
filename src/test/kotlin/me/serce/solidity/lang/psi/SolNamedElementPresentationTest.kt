package me.serce.solidity.lang.psi

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
    assertEquals("contracts.sol", location)
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
    assertEquals("nested/contracts.sol", location)
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
    assertEquals("free.sol", location)
  }
}
