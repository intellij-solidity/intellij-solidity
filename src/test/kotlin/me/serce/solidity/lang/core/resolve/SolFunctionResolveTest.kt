package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import org.intellij.lang.annotations.Language

class SolFunctionResolveTest : SolResolveTestBase() {
  fun testResolveFunction() = checkByCode("""
        contract B {
            uint public doit2;
        
            function doit2() {
                    //x
            }


            function doit() {
                doit2();
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
    val (refElement, _) = resolveInCode<SolFunctionCallExpression>("""
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
        
        contract ChildContract is SomeContract {
        
        }

        library Library {
            function something(SomeContract self, uint256 go) internal pure returns (uint256) {
                    //x
                return go;
            }
        }

        contract B {
            using Library for ChildContract;

            function doit(ChildContract value) {
                value.something(60);
                     //^
            }
        }
  """)

  fun testResolveUsingLibrary3() = checkByCode("""

        library Library {
            function findUpperBound(uint256[] storage array, uint256 element) internal view returns (uint256) {
                      //x
                return 0;
            }
        }

        contract B {
            using Library for uint256[];
            
            uint256[] private array;

            function doit(uint256  value) {
                array.findUpperBound(value);
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

  fun testResolveFunctionUintWithUnderscores() = checkByCode("""
        contract B {
            function doit(uint16) {
                    //x
            }

            function test() {
                doit(1_000);
                //^
            }
        }
  """)

  fun testResolveFunctionUintWithExponent() = checkByCode("""
        contract B {
            function doit(uint256) {
                    //x
            }

            function test() {
                doit(10 ** 18);
                //^
            }
        }
  """)

  fun testResolveFunctionUintWithScientificNotation() = checkByCode("""
        contract B {
            function doit(uint256) {
                    //x
            }

            function test() {
                doit(2e20);
                //^
            }
        }
  """)

  fun testResolveFunctionEnum() = checkByCode("""
        contract B {
            enum Test {
                ONE
            }

            function doit(Test) {
                    //x
            }

            function test() {
                doit(Test.ONE);
                //^
            }
        }
  """)

  fun testResolveWithCast() = checkByCode("""
        contract A {
            function doit2() {
                    //x
            }
        }

        contract B {
            function doit(address some) {
                A(some).doit2();
                       //^
            }
        }
  """)

  override fun checkByCode(@Language("Solidity") code: String) {
    checkByCodeInternal<SolFunctionCallExpression, SolNamedElement>(code)
  }
}
