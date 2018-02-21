package me.serce.solidity.lang.core.resolve

import junit.framework.Assert
import me.serce.solidity.lang.psi.SolNamedElement

class SolContractResolveTest : SolResolveTestBase() {
  fun testField() = checkByCode("""
        contract A {}
               //x

        contract B {
            A myField;
          //^
        }
  """)

  fun testInheritance() = checkByCode("""
        contract A {}
               //x

        contract B is A {
                    //^
        }
  """)

  fun testResolveUsingImport() {
    InlineFile(
      name = "a.sol",
      code = "contract a {}"
    )

    InlineFile("""
          import "./a.sol";

          contract b is a {}
                      //^
    """)

    val (refElement, _) = findElementAndDataInEditor<SolNamedElement>("^")
    val resolved = refElement.reference?.resolve()
    Assert.assertNotNull(resolved)
    Assert.assertEquals(resolved?.containingFile?.name, "a.sol")
  }

  fun testNotImported() {
    InlineFile(
      name = "a.sol",
      code = "contract a {}"
    )

    InlineFile("""
          import "./error.sol";

          contract b is a {}
                      //^
    """)

    val (refElement, _) = findElementAndDataInEditor<SolNamedElement>("^")
    Assert.assertNull(refElement.reference?.resolve())
  }
}
