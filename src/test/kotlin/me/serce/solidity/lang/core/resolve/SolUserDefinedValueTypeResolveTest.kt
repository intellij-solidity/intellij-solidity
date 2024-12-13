package me.serce.solidity.lang.core.resolve

class SolUserDefinedValueTypeResolveTest : SolResolveTestBase() {
  fun testUserDefinedValueTypeResolveInFile() = checkByCode("""
        type Decimal18 is uint256;
              //x

        interface MinimalERC20 {
            function transfer(address to, Decimal18 value) external;
                                            //^
        }
  """)

  fun testUserDefinedValueTypeResolveInAnInterface() = checkByCode("""
        interface MinimalERC20 {
            type Decimal18 is uint256;
               //x
              
            function transfer(address to, Decimal18 value) external;
                                            //^
        }
  """)

  fun testUserDefinedValueTypeResolveImported() = testResolveBetweenFiles(
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
                
          import {Decimal18} from "./a.sol";
                  //^
          contract b {
            Decimal18 public user;
          }
                      
    """
    )
  )

  fun testUserDefinedValueTypeResolveImported2() = testResolveBetweenFiles(
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
                
          import {Decimal18} from "./a.sol";
                  
          contract b {
            Decimal18 public user;
            //^
          }
                      
    """
    )
  )

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
