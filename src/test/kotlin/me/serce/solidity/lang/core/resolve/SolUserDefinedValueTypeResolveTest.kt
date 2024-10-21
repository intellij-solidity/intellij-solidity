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
}
