package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasLibraryResolveTest : SolResolveTestBase() {
  fun testResolveLibraryFromLibraryAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
            pragma solidity ^0.8.26;
                
            library a {
                  //x
                function doit() internal {
                }
            }
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
            pragma solidity ^0.8.26;
                
            import {a as A} from "./a.sol";
                     
            contract b {
                function test() public {
                    A.doit();
                  //^
                }
            }
    """
    )
  )

  fun testResolveLibraryFromFileAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
                
          library a {
                //x
            function doit() internal {
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
            function test() public {
                A.a.doit();
                //^
            }
          }
    """
    )
  )

  fun testResolveLibraryFromFileAliasWithMultipleLibraries() = testResolveBetweenFiles(
    InlineFile(
      code = """
            pragma solidity ^0.8.26;
                
            library a {
                function doit() internal {
                }
            }
            
            library ab {
                   //x
                function doit() internal {
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
                A.ab.doit();
                //^
            }
          }
    """
    )
  )

    fun testResolveLibraryFromAliasWithInheritance() {
        InlineFile(
            code = """
        pragma solidity ^0.8.0;

        import {Types as Constants} from "./types.sol";
        
        contract Parent {
            function foo() public pure returns (uint256) {
                return 42;
            }
        }
    """, name = "parent.sol"
        )

        testResolveBetweenFiles(
            InlineFile(
                code = """
        pragma solidity ^0.8.0;

        library Types {
                //x
            uint256 public constant VALUE = 256;
        }
    """, name = "types.sol"
            ), InlineFile(
                code = """
        pragma solidity ^0.8.0;

        import "./parent.sol";
        
        contract Child is Parent {
            function foo2() public pure returns (uint256) {
                return Constants.VALUE;
                          //^
            }
        }
    """, name = "child.sol"
            )
        )
    }
}
