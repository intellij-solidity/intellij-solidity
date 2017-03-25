package me.serce.solidity.lang.core.resolve

class SolModifierResolveTest : SolResolveTestBase() {
  fun testResolveModifier() = checkByCode("""
        contract B {
            modifier onlySeller() {
                     //x
                _;
            }

            function doit() internal onlySeller constant {
                                      //^
            }
        }
  """)

  fun testResolveMulti() = checkByCode("""
        contract B {
            modifier onlySeller1() {
            }

            modifier onlySeller2() {
                     //x
            }

            function doit() onlySeller1 onlySeller2 {
                                             //^
            }
        }
  """)
}
