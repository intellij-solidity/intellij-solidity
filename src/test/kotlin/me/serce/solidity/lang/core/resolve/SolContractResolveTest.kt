package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolNamedElement

class SolContractResolveTest : SolResolveTestBase() {

  fun testNewContractResolveToContractDefSelf() = checkByCode(
    """
        contract A {
               //x
          function resolve() {
            A a = new A();
                    //^
          }
        }
          """
  )

  fun testNewContractResolveToContractDefAnother() = checkByCode(
    """
        contract B {}
               //x
        contract A {
          function resolve() {
            B a = new B();
                    //^
          }
        }
          """
  )

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

  fun testResolveSymbolAliases() = testResolveBetweenFiles(
    InlineFile(
      code = """
          contract a {}
                 //x
      """,
      name = "a.sol"
    ),
    InlineFile("""
          import {a as A} from "./a.sol";

          contract b is A {}
                      //^
    """)
  )

  fun testResolveSymbolAliasesChain() {
    InlineFile(
      code = """
            contract d {}
        """,
      name = "d.sol"
    )
    InlineFile(
      code = """
            import {d as a} from "./d.sol";
            contract b {}
        """,
      name = "b.sol"
    )
    testResolveBetweenFiles(
      InlineFile(
        code = """
            import {b as B} from "./b.sol";
            contract a {}
                   //x
        """,
        name = "a.sol"
      ),
      InlineFile("""
            import {a as A} from "./a.sol";
                  //^
      """)
    )
  }

  fun testResolveUsingImport() = testResolveBetweenFiles(
    InlineFile(
      code = """
          contract a {}
                 //x
      """,
      name = "a.sol"
    ),
    InlineFile("""
          import "./a.sol";

          contract b is a {}
                      //^
    """)
  )

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
