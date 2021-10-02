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
}
