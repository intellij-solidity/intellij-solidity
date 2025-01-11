package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasUserDefinedValueResolveTest : SolResolveTestBase() {
  fun testResolveTypeWithChainedAliases() {
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
         
         type Decimal18 is uint256;
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
                B.A.Decimal18;
                    //^
            }
       }
  """
      )
    )
  }

  fun testUserDefinedValueTypeResolveAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         type Decimal18 is uint256;
                //x
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {Decimal18 as customType} from "./a.sol";

        contract C {
            function transfer(address to, customType value) external;
                                            //^
       }
  """
    )
  )

  fun testUserDefinedValueTypeResolveAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
           type Decimal18 is uint256;
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
            function transfer(address to, Types.Decimal18 value) external;
                                                //^
       }
  """
    )
  )
}
