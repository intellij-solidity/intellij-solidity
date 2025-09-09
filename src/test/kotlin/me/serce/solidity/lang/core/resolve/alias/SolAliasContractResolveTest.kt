package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasContractResolveTest : SolResolveTestBase() {
  fun testResolveContractFromFileAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
                
          contract a {
                 //x
            function doit() public {
            }
          }
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;
                
          import "./a.sol" as A;
                     
          contract b {
            function test(address x) public {
                A.a(x).doit();
                //^
            }
          }
    """
    )
  )

  fun testResolveContractFromFileAliasWithMultipleContracts() = testResolveBetweenFiles(
    InlineFile(
      code = """
            pragma solidity ^0.8.26;
                
            contract a {
                function doit() public {
                }
            }
            
            contract ab {
                   //x
                function doit() public {
                }
            }
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;
                
          import * as A from "./a.sol";
              
          contract b {
            function test(address x) public {
                A.ab(x).doit();
                 //^
            }
          }
    """
    )
  )

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
                //^
          contract b is A {}
                      
    """)
  )

  fun testResolveSymbolAliases2() = testResolveBetweenFiles(
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

  fun testResolveContractFromAliasWithInheritance() {
    InlineFile(
      code = """
        pragma solidity ^0.8.0;

        import {Types as Constants} from "./types.sol";
        
        contract Parent {
            function foo() public pure returns (uint256) {
                return 42;
            }
        }
    """,
      name = "parent.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
        pragma solidity ^0.8.0;

        library Types {
                //x
            uint256 public constant VALUE = 256;
        }
    """,
        name = "types.sol"
      ),
      InlineFile(
        code = """
        pragma solidity ^0.8.0;

        import "./parent.sol";
        
        contract Child is Parent {
            function foo2() public pure returns (uint256) {
                return Constants.VALUE;
                          //^
            }
        }
    """,
        name = "parent.sol"
      )
    )
  }
}
