package me.serce.solidity.lang.core.resolve

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

  fun testResolveSymbolAliases() {
    val aFile = InlineFile(
      code = "contract a {}",
      name = "a.sol"
    )

    InlineFile("""
          import {a as A} from "./a.sol";

          contract b is A {}
                      //^
    """)

    val (refElement, _) = findElementAndDataInEditor<SolNamedElement>("^")
    val resolved = refElement.reference?.resolve()
    assertNotNull(resolved)
    assertEquals(aFile.name, resolved?.containingFile?.name)
  }

  fun testResolveUsingImpor2t() {
    val aFile = InlineFile(
      code = "contract a {}",
      name = "a.sol"
    )

    InlineFile("""
          import "./a.sol";

          contract b is a {}
                      //^
    """)

    val (refElement, _) = findElementAndDataInEditor<SolNamedElement>("^")
    val resolved = refElement.reference?.resolve()
    assertNotNull(resolved)
    assertEquals(aFile.name, resolved?.containingFile?.name)
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
    assertNull(refElement.reference?.resolve())
  }
}
