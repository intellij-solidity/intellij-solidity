package me.serce.solidity.lang.completion

class SolKeywordCompletion : SolCompletionTestBase() {

  fun testRootCompletion() = checkCompletion(
    hashSetOf("pragma solidity", "pragma ", "library ", "contract ", "abstract ", "enum ", "struct", "event"), """
      /*caret*/
  """
  )

  fun testKeywordInContractCompletion() = checkCompletion(
    hashSetOf("function ", "mapping", "modifier", "struct", "this", "event", "enum", "fallback", "receive"), """
      contract A{
        /*caret*/
      }
  """
  )

  fun testContractKeyword() = checkCompletion(
    hashSetOf("contract ", "library "), """
        contract A{}
        /*caret*/
  """
  )

  fun testInFunctionKeywords() = checkCompletion(
    hashSetOf("this", "return", "while", "assembly", "assert", "require", "revert", "super"), """
        contract A {
            function test() {
                /*caret*/
            }
        } 
  """
  )

  fun testOnFunctionKeywords() = checkCompletion(
    hashSetOf("external ", "internal ", "public ", "private ", "payable", "pure" , "view"), """
        contract A {
            function test() /*caret*/ 
        } 
  """
  )

  fun testThisKeywordInMemberAccess() = checkCompletion(
    hashSetOf("this"), """
        contract A {
            address a;
            
            function test() {
                a.transfer(/*caret*/);
            }
        } 
  """
  )

  fun testKeywordsVariable() = checkCompletion(
    hashSetOf("constant ", "external ", "internal ", "public ", "private "), """
        contract A{
            uint256 /*caret*/
        }
        
  """
  )

  fun testThisKeywordNotInMemberAccess() = checkCompletion(
    hashSetOf("call", "code", "delegatecall", "transfer", "balance", "codehash", "send", "staticcall"), """
        contract A {
            address a;
        
            function test() {
                a./*caret*/
            }
        } 
  """, strict = true
  )

  fun testBreakInWhile() = checkCompletion(
    hashSetOf("break"), """
    contract A {
      function example() {
        while(true) {
        /*caret*/
        }
      }
    }
  """
  )

  fun testBreakInFor() = checkCompletion(
    hashSetOf("break"), """
    contract A {
      function example() {
        for(uint256 i = 0; i < 1000; i++) {
        /*caret*/
        }
      }
    }
  """
  )
}
