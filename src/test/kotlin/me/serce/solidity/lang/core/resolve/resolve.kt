package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolResolveTestBase : SolTestBase() {
  protected fun checkByCode(@Language("Solidity") code: String) {
    InlineFile(code)

    val (refElement, data) = findElementAndDataInEditor<SolNamedElement>("^")

    if (data == "unresolved") {
      val resolved = refElement.reference?.resolve()
      check(resolved == null) {
        "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
      }
      return
    }

    val references = refElement.references
    assertTrue("Failed to resolve ${refElement.text}", references.isNotEmpty())
    val target = findElementInEditor<SolNamedElement>("x")

    if(references.size ==  1) {
      assertEquals(target, references.first())
    } else {
      assertTrue(references.map { it?.resolve() }.filterNotNull().contains(target))
    }
  }

  protected fun assertCode() {
    val (refElement, data) = findElementAndDataInEditor<SolNamedElement>("^")

    if (data == "unresolved") {
      val resolved = refElement.reference?.resolve()
      check(resolved == null) {
        "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
      }
      return
    }

    val resolved = checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    val target = findElementInEditor<SolNamedElement>("x")

    assertEquals(target, resolved)
  }

}


