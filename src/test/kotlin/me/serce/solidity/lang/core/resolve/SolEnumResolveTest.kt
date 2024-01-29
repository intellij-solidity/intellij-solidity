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

  // TODO: implement top level enum resolution
  /*
  fun testEnumResolveInFile() = checkByCode("""
      enum B { A1, A2 }
         //x

      contract A {
          function f() public {
              B.A2;
            //^
          }
      }
  """)
  */

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

  fun testEnumItself2() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x

            function f(B test) {
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

  fun testEnumParent2() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x
        }

        contract C is A {
            function f(B test) {
                     //^
            }
        }
  """)

  fun testEnumFromOtherContract() = checkByCode("""
        contract A {
            enum B { A1, A2 }
               //x
        }

        contract C {
            function f(A.B test) {
                       //^
            }
        }
  """)

  fun testEnumValueFromOtherContract() = checkByCode("""
        contract A {
            enum B { A1, A2 }
                   //x
        }

        contract C {
            function f() {
                A.B test = A.B.A1;
                              //^
            }
        }
  """)
}
