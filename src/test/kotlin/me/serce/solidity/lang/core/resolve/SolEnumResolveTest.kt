package me.serce.solidity.lang.core.resolve

class SolEnumResolveTest : SolResolveTestBase() {
  fun testEnumResolve() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                        //x

            function f() {
                B.A2;
                //^
            }
        }
  """)

  fun testEnumItself() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x

            function f() {
                B.A2;
              //^
            }
        }
  """)

  fun testEnumParent() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                    //x
        }

        contract C is A {
            function f() {
                B.A1;
                //^
            }
        }
  """)

}
