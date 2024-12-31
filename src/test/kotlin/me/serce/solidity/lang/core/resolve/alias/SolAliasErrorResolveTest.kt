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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Closed as ErrorClosed} from "./a.sol";

        contract C {
            function f() public {
               revert ErrorClosed();
                        //^
            }
       }
  """)
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
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
               revert Types.Closed();
                            //^
            }
       }
  """)
  )
}
