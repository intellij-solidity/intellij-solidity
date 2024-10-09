package me.serce.solidity.lang.core.resolve

import com.intellij.openapi.util.RecursionManager
import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolNamedElement

class SolEventResolveTest : SolResolveTestBase() {

  fun testEventWithNoArguments() = checkByCode(
    """
       pragma solidity ^0.8.26;
       
        contract B {
            event Closed();
                    //x
            function close() public {
                emit Closed();
                    //^
            }
        }
  """
  )

  fun testEventParent() = checkByCode(
    """
       pragma solidity ^0.8.26;
       
        contract A {
            event Closed();
                    //x
        }

        contract B is A {
            function close() public {
                emit Closed();
                    //^
            }
        }
  """
  )

  fun testEventWithParameters() = checkByCode(
    """
       pragma solidity ^0.8.26;
       
        contract B {
            event Refunded(int a, uint256 b);
                    //x

            function close() public {
                emit Refunded(1, 2);
                      //^
            }
        }
  """
  )

  fun testContractTheSameNameAsEvent() {
    // TODO: the test below fails on the latest Solidity compiler with
    //    > SyntaxError: Functions are not allowed to have the same name as the contract.
    //    > If you intend this to be a constructor, use "constructor(...) { ... }" to define it.
    //    In this situation, resolve works incorrectly. However, what the test actually
    //    verifies is that  a StackOverflowException isn't thrown,
    //    see https://github.com/intellij-solidity/intellij-solidity/issues/309
    // A recursion guard check is disabled to prevent c.i.o.u.RecursionManager.CachingPreventedException
    // from being thrown. The case highlights another issue that's worth fixing.
    RecursionManager.disableMissedCacheAssertions(testRootDisposable)
    checkByCode(
      """
         pragma solidity ^0.8.26;
         
        contract B {
            event B();
                   
            function close() public {
                emit B();
                   //^
            }
            
            function B() public {
                   //x
            }
        }
    """
    )
  }

  fun testEventAtFileLevel() = checkByCode(
    """
        pragma solidity ^0.8.26;
        
        event Refunded(int a, uint256 b);
                //x
        contract B {

            function close() public {
                emit Refunded(1, 2);
                    //^
            }
        }
  """
  )

  fun testResolveImportedEvent() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
          event Closed();
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
              function emitEvent() public {
                emit Closed();
              }
          }
                      
    """
    )
  )

  fun testResolveImportedEvent2() = testResolveBetweenFiles(
    InlineFile(
      code = """
          pragma solidity ^0.8.26;
          
          event Closed();
                  //x
      """,
      name = "a.sol"
    ),
    InlineFile(
      """
          pragma solidity ^0.8.26;      
                
          import {Closed} from "./a.sol";
                  
          contract b {
              function emitEvent() public {
                emit Closed();
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
