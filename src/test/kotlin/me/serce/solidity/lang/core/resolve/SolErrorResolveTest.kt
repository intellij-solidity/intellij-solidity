package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.types.SolErrorType

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

  fun testErrorWithParameters() = checkByCode("""
        contract B {
            error Refunded(int a, uint256 b);
                    //x

            function close() {
                revert Refunded(1, 2);
                     //^
            }
        }
  """)

  fun testErrorInRequire() = checkByCode("""
        pragma solidity ^0.8.27;

        contract B {
            error InsufficientBalance(uint available, uint required);
                    //x

            function transfer(uint available, uint required) public pure {
                require(available >= required, InsufficientBalance(available, required));
                                               //^
            }
        }
  """)

  fun testErrorWithNamedArgumentsInRequire() = checkByCode("""
        pragma solidity ^0.8.27;

        contract B {
            error InsufficientBalance(uint available, uint required);
                    //x

            function transfer(uint available, uint required) public pure {
                require(available >= required, InsufficientBalance({required: required, available: available}));
                                               //^
            }
        }
  """)

  fun testRequireResolvesCustomErrorOverload() {
    val (call, _) = resolveInCode<SolFunctionCallExpression>("""
        pragma solidity ^0.8.27;

        error Unauthorized();

        contract B {
            function transfer(bool allowed) public pure {
                require(allowed, Unauthorized());
                //^
            }
        }
    """)

    val resolved = call.reference?.resolve() as? SolFunctionDefinition
    assertNotNull(resolved)
    assertEquals(SolErrorType, resolved!!.parseParameters()[1].second)
  }

  fun testRequireRejectsNonErrorSecondArgument() = checkByCode("""
        pragma solidity ^0.8.27;

        contract B {
            function transfer() public pure {
                require(true, 42);
                //^ unresolved
            }
        }
  """)

  fun testRequireRejectsInvalidCustomErrorArguments() = checkByCode("""
        pragma solidity ^0.8.27;

        contract B {
            error Unauthorized(address caller);

            function transfer() public pure {
                require(true, Unauthorized());
                //^ unresolved
            }
        }
  """)

  fun testErrorAtFileLevel() = checkByCode(
    """
        pragma solidity ^0.8.26;
        
        error Refunded(int a, uint256 b);
                //x
        contract B {
            function close() public {
                revert Refunded(1, 2);
                       //^
            }
        }
  """
  )

  fun testResolveImportedError() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
          error Closed();
                  //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {Closed} from "./a.sol";
                  //^
          contract b {
              function close() public {
                revert Closed();
              }
          }
                      
    """
    )
  )

  fun testResolveImportedError2() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
          error Closed();
                  //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {Closed} from "./a.sol";
                  
          contract b {
              function close() public {
                revert Closed();
                      //^
              }
          }
                      
    """
    )
  )

  override fun checkByCode(code: String) {
    checkByCodeInternal<SolFunctionCallExpression, SolNamedElement>(code)
  }
}
