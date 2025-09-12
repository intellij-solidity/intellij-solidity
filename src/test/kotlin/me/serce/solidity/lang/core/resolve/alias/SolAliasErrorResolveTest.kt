package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasErrorResolveTest : SolResolveTestBase() {
  fun testResolveErrorFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          error Closed();
                 //x
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {Closed as ErrorClosed} from "./a.sol";

        contract C {
            function f() public {
               revert ErrorClosed();
                        //^
            }
       }
  """
    )
  )

  fun testResolveErrorFromAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            error Closed();
                 //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
               revert Types.Closed();
                            //^
            }
       }
  """
    )
  )

  fun testResolveErrorWithChainedAliases() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import {Closed as A} from "./a.sol";
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         error Closed();
              //x
      """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                revert B.A;
                       //^
            }
       }
  """
      )
    )
  }

  fun testResolveErrorWithChainedFileAlias() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
         error Closed();
              //x
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
                revert B.A.Closed();
                          //^
            }
       }
  """
      )
    )
  }

  fun testResolveErrorInInterfaceWithChainedFileAlias() {
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         import "./a.sol" as A;
         
    """,
      name = "b.sol"
    )

    testResolveBetweenFiles(
      InlineFile(
        code = """
         pragma solidity ^0.8.26;
         
        interface InterfaceTest{
            error errorB();
                    //x
        }
    """,
        name = "a.sol"
      ),
      InlineFile(
        """
        pragma solidity ^0.8.26;
        
        import "./b.sol" as B;

        contract C {
            function f() public {
               revert B.A.InterfaceTest.errorB();
                                        //^
            }
       }
  """
      )
    )
  }

    fun testResolveErrorFromAliasWithInheritance() {
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

            interface Types {
                error errorB();
                    //x
            }
            """, name = "types.sol"
            ), InlineFile(
                code = """
            pragma solidity ^0.8.0;

            import "./parent.sol";
            
             contract Child is Parent {
                function foo2() public {
                    revert Constants.errorB();
                                       //^
                }
            }
            """, name = "child.sol"
            )
        )
    }
}
