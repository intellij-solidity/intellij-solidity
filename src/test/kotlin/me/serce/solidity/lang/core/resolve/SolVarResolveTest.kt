package me.serce.solidity.lang.core.resolve

class SolVarResolveTest : SolResolveTestBase() {
  fun testLocal1() = checkByCode("""
        contract B {
            function B() {
                var a0 = 0;
                var a = 0;
                  //x
                var b = 1;
                a = a++;
              //^
            }
        }
  """)

  fun testLocal2() = checkByCode("""
        contract B {
            function B() {
                var a0 = 0;
                var a = 0;
                  //x
                var b = 1;
                a = a++;
                  //^
            }
        }
  """)

  fun testFieldResolve1() = checkByCode("""
        contract B {
            uint public lastC;
                        //x

            function B() {
                lastC = 1;
                //^
            }
        }
  """)
}
