package me.serce.solidity.lang.completion

class SolKeywordCompletion : SolCompletionTestBase() {

  fun testRootCompletion() = checkCompletion(
    hashSetOf("pragma solidity", "pragma ", "library ", "contract "), """
      /*caret*/
  """
  )

  fun testContractKeyword() = checkCompletion(
    hashSetOf("contract ", "library "), """
        contract A{}
        /*caret*/
  """
  )

  fun testThisKeyword() = checkCompletion(
    hashSetOf("this"), """
        contract A {
            function test() {
                /*caret*/
            }
        } 
  """)

  fun testThisKeywordInMemberAccess() = checkCompletion(
    hashSetOf("this"), """
        contract A {
            address a;
            
            function test() {
                a.transfer(/*caret*/);
            }
        } 
  """)

  fun testThisKeywordNotInMemberAccess() = checkCompletion(
    hashSetOf("call", "code", "delegatecall", "transfer", "balance", "codehash", "send", "staticcall"), """
        contract A {
            address a;
        
            function test() {
                a./*caret*/
            }
        } 
  """, strict = true)

}
