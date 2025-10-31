package me.serce.solidity.lang.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.utils.SolTestBase

class SolNamedElementPresentationTest : SolTestBase() {

  fun testFunctionPresentation() {
    val psiFile = InlineFile(
      """
      contract C {
          function foo() {}
      }
      """.trimIndent(),
      "contracts.sol"
    ).psiFile

    checkFunctionPresentation(psiFile,"foo", "contracts.sol")
  }

  fun testFunctionPresentationFromSubdirectory() {
    val psiFile = myFixture.addFileToProject(
      "nested/contracts.sol",
      """
      contract C {
          function foo() {}
      }
      """.trimIndent()
    )

    checkFunctionPresentation(psiFile,"foo", "nested/contracts.sol")
  }

  fun testFreeFunctionPresentation() {
    val psiFile = InlineFile(
      """
      function bar() {}
      """.trimIndent(),
      "free.sol"
    ).psiFile

      checkFunctionPresentation(psiFile,"bar", "free.sol")
  }

    private fun checkFunctionPresentation(
        psiFile: PsiFile, elementToSearch: String, expectedLocation: String
    ) {
        val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
            .single { it.name == elementToSearch }

        val presentation = function.presentation
        val location = presentation?.locationString

        assertNotNull("Expected non-null presentation location for function bar", location)
        assertEquals(expectedLocation, location)
    }
}
