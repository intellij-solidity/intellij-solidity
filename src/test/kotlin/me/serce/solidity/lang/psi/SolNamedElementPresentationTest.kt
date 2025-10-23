package me.serce.solidity.lang.psi

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class SolNamedElementPresentationTest : SolTestBase() {

  fun testFunctionPresentationIncludesContractAndPath() {
    val psiFile = InlineFile(
      """
      contract C {
          function foo() {}
      }
      """.trimIndent(),
      "contracts/C.sol"
    ).psiFile

    val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
      .single { it.name == "foo" }

    val presentation = function.presentation
    val location = presentation?.locationString

    assertNotNull("Expected non-null presentation location for function foo", location)
    assertTrue("Location should include contract name prefix", location!!.startsWith("C - "))

    val path = FileUtil.toSystemIndependentName(location.substringAfter("C - "))
    assertTrue(
      "Location should point to contracts/C.sol but was $path",
      path.endsWith("contracts/C.sol")
    )
  }

  fun testFreeFunctionPresentationShowsFilePathOnly() {
    val psiFile = InlineFile(
      """
      function bar() {}
      """.trimIndent(),
      "lib/free.sol"
    ).psiFile

    val function = PsiTreeUtil.collectElementsOfType(psiFile, SolFunctionDefinition::class.java)
      .single { it.name == "bar" }

    val presentation = function.presentation
    val location = presentation?.locationString

    assertNotNull("Expected non-null presentation location for function bar", location)

    val normalized = FileUtil.toSystemIndependentName(location!!)
    assertTrue(
      "Location should point to lib/free.sol but was $normalized",
      normalized.endsWith("lib/free.sol")
    )
    assertFalse("Location should not include contract prefix for free function", location.contains(" - "))
  }
}
