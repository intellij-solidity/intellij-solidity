package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasEventResolveTest : SolResolveTestBase() {
  fun testResolveEventFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          event Closed();
                 //x    
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Closed as EventClosed} from "./a.sol";

        contract C {
            function f() public {
               emit EventClosed();
                        //^
            }
       }
  """)
  )

  fun testResolveEventFromAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          interface InterfaceI {
            event Closed();
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
               emit Types.Closed();
                           //^
            }
       }
  """)
  )
}
