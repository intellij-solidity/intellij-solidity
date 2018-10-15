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

  fun testField1() = checkByCode("""
        contract B {
            uint public lastC;
                        //x

            function B() {
                lastC = 1;
                //^
            }
        }
  """)

  fun testFunctionParams() = checkByCode("""
        contract B {
            function B(uint abc) {
                          //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testFunctionParamsSecond() = checkByCode("""
        contract B {
            function B(uint abc1, uint abc2) {
                                       //x
                abc1 = 1;
                abc2 = 1;
                //^
            }
        }
  """)

  fun testConstructorParams() = checkByCode("""
        contract B {
            constructor(uint abc) {
                          //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testReturnVars() = checkByCode("""
        contract B {
            function test() returns (uint abc) {
                                         //x
                _;
                abc = 1;
                //^
            }
        }
  """)

  fun testTupleVar() = checkByCode("""
        contract B {
            function test() {
                var (var1, var2) = (5, 5);
                     //x
                _;
                var1 = 1;
                //^
            }
        }
  """)

  fun testResolveStateInheritance() = checkByCode("""
        contract C {
            uint abc;
                //x
        }

        contract B {}

        contract A is B, C {
            function A() {
                abc = 1;
                //^
            }
        }
  """)

  fun testResolveThis() = checkByCode("""
        contract B {
            function doit2() {
                    //x
            }


            function doit() {
                this.doit2();
                     //^
            }
        }
  """)

  fun testResolveSuper() = checkByCode("""
        contract B {
            var b;
              //x
        }

        contract A is B {
            function doit() {
                super.b;
                    //^
            }
        }
  """)
}
