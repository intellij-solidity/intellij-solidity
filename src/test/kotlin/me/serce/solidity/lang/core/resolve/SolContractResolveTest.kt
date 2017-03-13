package me.serce.solidity.lang.core.resolve

class SolContractResolveTest : SolResolveTestBase() {
  fun testField() = checkByCode("""
        contract A {}
               //x

        contract B {
            A myField;
          //^
        }
  """)

  fun testInheritance() = checkByCode("""
        contract A {}
               //x

        contract B is A {
                    //^
        }
  """)
}
