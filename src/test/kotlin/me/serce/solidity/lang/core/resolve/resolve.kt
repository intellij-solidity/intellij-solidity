package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolReferenceElement
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

abstract class SolResolveTestBase : SolTestBase() {
  protected fun checkByCode(@Language("Solidity") code: String) {
    InlineFile(code)

    val (refElement, data) = findElementAndDataInEditor<SolReferenceElement>("^")

    if (data == "unresolved") {
      val resolved = refElement.reference.resolve()
      check(resolved == null) {
        "$refElement `${refElement.text}`should be unresolved, was resolved to\n$resolved `${resolved?.text}`"
      }
      return
    }

    val resolved = checkNotNull(refElement.reference.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

    val target = findElementInEditor<SolNamedElement>("x")

    assertEquals(target, resolved)
  }
}


