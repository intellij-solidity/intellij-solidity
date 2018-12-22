package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement

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

  fun testResolveGlobal() {
    val (refElement, _) = resolveInCode<SolNamedElement>("""
        contract B {
            function doit() {
                assert(true);
                 //^
            }
        }
    """)

    val resolved = refElement.reference?.resolve()
    assertTrue(resolved is SolFunctionDefinition)
    if (resolved is SolFunctionDefinition) {
      assertEquals(resolved.name, "assert")
    }
  }

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

  fun testResolveUsingLibrary1() = checkByCode("""
        library Library {
            function something(bytes self, uint256 go) internal pure returns (uint256) {
                    //x
                return go;
            }
        }

        contract B {
            using Library for bytes;

            function doit(bytes value) {
                value.something(60);
                     //^
            }
        }
  """)

  fun testResolveUsingLibraryWithInheritance() = checkByCode("""
        library Library {
            function something(bytes self, uint256 go) internal pure returns (uint256) {
                    //x
                return go;
            }
        }

        contract Super {
            using Library for bytes;
        }

        contract B is Super {
            using Library for bytes;

            function doit(bytes value) {
                value.something(60);
                     //^
            }
        }
  """)


  fun testResolveUsingLibrary2() = checkByCode("""
        contract SomeContract {}

        library Library {
            function something(SomeContract self, uint256 go) internal pure returns (uint256) {
                    //x
                return go;
            }
        }

        contract B {
            using Library for SomeContract;

            function doit(SomeContract value) {
                value.something(60);
                     //^
            }
        }
  """)

  fun testResolveUsingLibraryWithWildcard() = checkByCode("""
        library Library {
            function something(bytes self, uint256 go) internal pure returns (uint256) {
                    //x
                return go;
            }
        }

        contract B {
            using Library for *;

            function doit(bytes value) {
                value.something(60);
                     //^
            }
        }
  """)

  fun testResolveFunctionSameNumberOfArguments() = checkByCode("""
        contract B {
            function doit(uint16) {
                    //x
            }

            function doit(string) {

            }

            function test() {
                doit(1 + 1);
                //^
            }
        }
  """)
}
