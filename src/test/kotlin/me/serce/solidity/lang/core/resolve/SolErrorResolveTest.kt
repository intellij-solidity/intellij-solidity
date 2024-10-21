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
