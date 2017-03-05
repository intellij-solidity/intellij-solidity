package me.serce.solidity.lang.core.resolve

class SolContractResolveTest : SolResolveTestBase() {
  fun testTwoContracts() = checkByCode("""
        contract A {}
               //x

        contract B {
            A myField;
          //^
        }
  """)
}
