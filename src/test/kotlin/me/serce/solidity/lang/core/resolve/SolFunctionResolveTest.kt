package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.psi.SolVarLiteral
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
        contract Base {
            function doit2() {
            }
        }

        contract A is Base {
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
            function doit(address adr) {
                A a = A(adr);
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

  fun testResolveVarAsFunction() = checkByCodeInternal<SolVarLiteral, SolNamedElement>("""
        contract B {
            uint256 doit;
        
            function doit(uint16) {
                    //x
            }

            function test() {
                doit(1 + 1);
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

  fun testResolveFreeFunctions() = checkByCode("""
        function min(uint x, uint y) pure returns (uint) {
                //x
            return x < y ? x : y;
        }
        
        contract B {
            function test() public {
                min(1, 2);
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

  fun testResolvePublicVar() = checkByCode("""
        contract A {
            uint public value;
                       //x
        }

        contract B {
            function doit(address some) {
                A(some).value();
                       //^
            }
        }
  """)

  fun testResolveTransfer() {
    checkIsResolved("""
        contract B {
            function doit(address some) {
                some.transfer(100);
                       //^
            }
        }
  """)
  }

  fun testResolveArrayPush() {
    checkIsResolved("""
        contract B {
            function doit(uint256[] storage array) {
                array.push(100);
                     //^
            }
        }
  """)
  }

  fun testResolveImportedError() {
    val file1 = InlineFile("""
        error XyzError(uint x);
                //x
      """,
      name = "Xyz.sol"
    )

    val file2 = InlineFile("""
        import "./Xyz.sol";
        contract B { 
            function doit(uint256[] storage array) {
                revert XyzError(1);
                        //^
            }
        }
    """)

    testResolveBetweenFiles(file1, file2)
  }

    fun testResolveImportedFunction() = testResolveBetweenFiles(
        InlineFile(
            code = """
        pragma solidity ^0.8.26;

        contract a {
            function doit() public {
                    //x
            }
        }
  """,
            name = "a.sol"
        ),
        InlineFile(
            """
        pragma solidity ^0.8.26;

        import {a} from "./a.sol";
        
        contract b {
            function test(address x) public {
                a(x).doit();
                    //^
            }
        }
"""
        )
    )

    fun testResolveImportedContractFunction() = testResolveBetweenFiles(
        InlineFile(
            code = """
      pragma solidity ^0.8.26;

        contract a {
               //x
            function doit() public {
            }
        }
  """,
            name = "a.sol"
        ),
        InlineFile(
            """
        pragma solidity ^0.8.26;

        import {a} from "./a.sol";
        
        contract b {
            function test(address x) public {
                a(x).doit();
              //^
            }
        }
"""
        )
    )

    fun testResolveImportedFunctionFromLibrary() = testResolveBetweenFiles(
        InlineFile(
            code = """
          pragma solidity ^0.8.26;
                
          library a {
            function doit() internal {
                     //x
            }
          }
      """,
            name = "a.sol"
        ),
        InlineFile(
            """
          pragma solidity ^0.8.26;
                
          import "./a.sol";
              
          contract b {
            function test() public {
                a.doit();
                  //^
            }
          }
    """
        )
    )

  fun testResolveFunctionWithModifier() = checkByCode("""
        pragma solidity ^0.8.26;

        contract Temp {
            modifier onlyOwner() {
                _;
            }
        
            function foo() public onlyOwner {
                    //x   
            }
        
            function bar() external {
                foo();
                //^
            }
        }
  """)

  fun testResolveFunctionWithTypeAlias() = checkByCode("""
    type Foo is uint256;

    library FooLib {
        function isHappy(Foo f) internal pure returns(bool) {
                    //x   
            return Foo.unwrap(f) > 100;
        }
    }
    
    using {
        FooLib.isHappy
    } for Foo global;
    
    library TestLib {
        function doStuff() internal pure {
            Foo foo = Foo.wrap(17);
            if (foo.isHappy()) {
                    //^
            }
        }
    }
  """)

  fun checkIsResolved(@Language("Solidity") code: String) {
    val (refElement, _) = resolveInCode<SolFunctionCallExpression>(code)
    assertNotNull(refElement.reference?.resolve())
  }

  override fun checkByCode(@Language("Solidity") code: String) {
    checkByCodeInternal<SolFunctionCallExpression, SolNamedElement>(code)
  }
}
