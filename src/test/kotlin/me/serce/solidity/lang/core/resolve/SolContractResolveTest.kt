package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolElement
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

  fun testNewResolveToConstructor() = checkByCodeSearchType<SolElement>(
    """
        contract B {
          constructor() {
               //x
          }
        }
        contract A {
          function resolve() {
            B a = new B();
                    //^
          }
        }
          """
  )

  fun testNewResolveToConstructorFunction() = checkByCodeSearchType<SolElement>(
    """
        contract B {
          function B() {
                 //x
          }
        }
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

  fun testResolveWithCast() = testResolveBetweenFiles(
      InlineFile(
          code = """
          contract A {
                 //x
              function doit2() {
              }
          }
    """,
          name = "a.sol"
      ),
      InlineFile("""
        import "./a.sol";

        contract B {
          function doit(address some) {
              A(some).doit2();
            //^
          }
       }
  """)
  )

    fun testResolveContractWithInheritance() = testResolveBetweenFiles(
        InlineFile(
            code = """
        pragma solidity ^0.8.0;

        contract Parent {
                //x
            uint256 public constant VALUE = 256;
        }
    """, name = "parent.sol"
        ), InlineFile(
            code = """
        pragma solidity ^0.8.0;

        import "./parent.sol";
        
        contract Child is Parent {
            function foo2() public pure returns (uint256) {
                return Parent.VALUE;
                          //^
            }
        }
    """, name = "child.sol"
        )
    )

    fun testResolveInheritedContractToOnlyOneReference() {
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        contract Ownable {
            modifier onlyOwner() {
                _;
            }
        }
    """, name = "ownableOfLib.sol"
        )
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        import "./ownableOfLib.sol" as ownableOfLib;
        
        library someLib {
        
        }
    """, name = "someLib.sol"
        )
        testResolveBetweenFiles(
            InlineFile(
                code = """
        pragma solidity ^0.8.10;

        abstract contract Ownable {
                           //x
            modifier onlyOwner() {
                _;
            }
        }
    """, name = "ownable.sol"
            ),
            InlineFile(
                code = """
        pragma solidity ^0.8.10;
  
        import "./ownable.sol";
        import "./someLib.sol";
        
        contract main is Ownable {
                          //^
            constructor(){
            }
        
            function foo() public onlyOwner {
            }
        }
      """, name = "main.sol"
            )
        )
    }

    fun testResolveInheritedContractToOnlyOneReference2() {
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        import "./ownableOfLib.sol" as ownableOfLib;
        
        library someLib {
        
        }
    """, name = "someLib.sol"
        )
        InlineFile(
            code = """
        pragma solidity ^0.8.10;

        abstract contract Ownable {
            modifier onlyOwner() {
                _;
            }
        }
    """, name = "ownable.sol"
        )
        testResolveBetweenFiles(
            InlineFile(
                code = """
        pragma solidity ^0.8.10;

        contract Ownable {
                   //x
            modifier onlyOwner() {
                _;
            }
        }
    """, name = "ownableOfLib.sol"
            ),
            InlineFile(
                code = """
        pragma solidity ^0.8.10;
  
        import "./ownable.sol";
        import "./someLib.sol";
        
        contract main is ownableOfLib.Ownable {
                                     //^
            constructor(){
            }
        
            function foo() public onlyOwner {
            }
        }
      """, name = "main.sol"
            )
        )
    }
}
