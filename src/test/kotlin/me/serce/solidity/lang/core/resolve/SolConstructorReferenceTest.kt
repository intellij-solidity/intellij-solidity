package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.utils.SolTestBase

class SolConstructorReferenceTest: SolTestBase() {
  fun testConstructorResolve() {
    InlineFile("""
          contract c {
            function c() {
                   //^

            }
          }
    """)

    val (refElement) = findElementAndDataInEditor<SolFunctionDefinition>("^")
    checkNotNull(refElement.reference?.resolve()) {
      "Failed to resolve ${refElement.text}"
    }

  }

}
