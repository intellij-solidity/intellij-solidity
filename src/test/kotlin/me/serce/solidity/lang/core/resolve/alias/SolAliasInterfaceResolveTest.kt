package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasInterfaceResolveTest : SolResolveTestBase() {
  fun testResolveInterfaceFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
                     //x
            enum B { A1, A2 }
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
                Types.B.A2;
                //^
            }
       }
  """)
  )

  fun testResolveInterfaceFromFileAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
                     //x
            enum B { A1, A2 }
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import "./a.sol" as A;

        contract C {
            function f() public {
                A.InterfaceI.B.A2;
                 //^
            }
       }
  """)
  )

  fun testResolveInterfaceWithChainedFileAlias() {
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
                    //x
            error errorB();
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
}
