package me.serce.solidity.lang.core.resolve.alias

import me.serce.solidity.lang.core.resolve.SolResolveTestBase

class SolAliasVarResolveTest : SolResolveTestBase() {
  fun testResolveConstantFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          address constant CONSTANT_A = address(0x1111111111111111111111111111111111111111);
                            //x
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {CONSTANT_A as USER_1} from "./a.sol";

        contract b {
            address public user;
              function setUser() public {
                user = USER_1;
                         //^ 
              }
          }
  """
    )
  )

  fun testResolveConstantFromAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            address constant CONSTANT_A = address(0x1111111111111111111111111111111111111111);
                              //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract b {
            address public user;
              function setUser() public {
                user = Types.CONSTANT_A;
                            //^ 
              }
          }
  """
    )
  )

  fun testResolveConstantFromFileAliasInInterface() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            address constant CONSTANT_A = address(0x1111111111111111111111111111111111111111);
                              //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import "./a.sol" as A;

        contract b {
            address public user;
              function setUser() public {
                user = A.InterfaceI.CONSTANT_A;
                                    //^ 
              }
          }
  """
    )
  )

  fun testResolveVarFromFileAliasInContract() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          contract a {
            address public user;
                          //x
          }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import "./a.sol" as A;

        contract b {
          address public user = A.a(address(0)).user();
                                                //^ 
        }
  """
    )
  )

  fun testResolveVarFromAliasInContract() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          contract a {
            address public user;
                          //x
          }
    """,
      name = "a.sol"
    ),
    InlineFile(
      """
        pragma solidity ^0.8.26;
        
        import {a as ContractA} from "./a.sol";

        contract b {
          address public user = ContractA(address(0)).user();
                                                    //^ 
        }
  """
    )
  )
}
