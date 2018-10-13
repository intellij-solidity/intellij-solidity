package me.serce.solidity.lang.core.resolve

class SolFunctionResolveTest : SolResolveTestBase() {
  fun testResolveFunction() = checkByCode("""
        contract B {
            function doit2() {
                    //x
            }


            function doit() {
                doit2();
                //^
            }
        }
  """)

  fun testResolveFunctionWithParameters() = checkByCode("""
        contract B {
            function doit2(int a, int b) {
                    //x
            }


            function doit() {
                doit2(1, 2);
                //^
            }
        }
  """)

  fun testResolveFunctionFromParent() = checkByCode("""
        contract A {
            function doit2() {
                    //x
            }
        }

        contract B is A {
            function doit() {
                doit2();
                //^
            }
        }
  """)

  fun testResolveFunctionUsingSuper() = checkByCode("""
        contract Parent1 {
        }

        contract Parent2 {
            function doSomething() {
                    //x
            }
        }

        contract B is Parent1, Parent2 {
            function doSomething() {
                super.doSomething();
                     //^
            }
        }
  """)

  fun testResolveContractProperty() = checkByCode("""
        contract A {
            function doit2() {
                    //x
            }
        }

        contract B {
            A a;

            function doit() {
                a.doit2();
                  //^
            }
        }
  """)

  fun testResolveContractConstructor() = checkByCode("""
        contract A {
               //x
        }

        contract B {
            function doit() {
                A a = A(1);
                    //^
            }
        }
  """)
}
