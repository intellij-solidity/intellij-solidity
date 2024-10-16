package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolNamedElement

class SolErrorResolveTest : SolResolveTestBase() {
  fun testErrorWithNoArguments() = checkByCode("""
        contract B {
            error Closed();
                    //x
            function close() {
                revert Closed();
                     //^
            }
        }
  """)

  fun testErrorParent() = checkByCode("""
        contract A {
            error Closed();
                    //x
        }

        contract B is A {
            function close() {
                revert Closed();
                     //^
            }
        }
  """)

  fun testErrorWithParemeters() = checkByCode("""
        contract B {
            error Refunded(int a, uint256 b);
                    //x

            function close() {
                revert Refunded(1, 2);
                     //^
            }
        }
  """)

  fun testResolveErrorFromAlias() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
          error Closed();
                 //x
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {Closed as ErrorClosed} from "./a.sol";

        contract C {
            function f() public {
               revert ErrorClosed();
                        //^
            }
       }
  """)
  )

  fun testResolveErrorFromAlias2() = testResolveBetweenFiles(
    InlineFile(
      code = """
         pragma solidity ^0.8.26;
         
         interface InterfaceI {
            error Closed();
                 //x
         }
    """,
      name = "a.sol"
    ),
    InlineFile("""
        pragma solidity ^0.8.26;
        
        import {InterfaceI as Types} from "./a.sol";

        contract C {
            function f() public {
               revert Types.ErrorClosed();
                            //^
            }
       }
  """)
  )

  override fun checkByCode(code: String) {
    checkByCodeInternal<SolFunctionCallExpression, SolNamedElement>(code)
  }
}
