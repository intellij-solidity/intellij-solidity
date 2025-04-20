package me.serce.solidity.lang.completion

class SolKeywordCompletion : SolCompletionTestBase() {

  fun testRootCompletion() = checkCompletion(
    hashSetOf("pragma solidity", "pragma ", "library ", "contract ", "abstract ", "enum ", "struct", "event", "error", "using", "type"), """
      /*caret*/
  """
  )

  fun testKeywordInContractCompletion() = checkCompletion(
    hashSetOf("function ", "mapping", "modifier", "struct", "this", "event", "enum", "fallback", "receive", "constructor", "error","using", "type"), """
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
    hashSetOf("external ", "internal ", "public ", "private ", "payable", "pure", "view", "virtual", "override", "returns"), """
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
    hashSetOf("constant ", "internal ", "public ", "private ", "override ", "immutable ", "transient "),
    """
        contract A{
            uint256 /*caret*/
        }
        
  """
  )

  fun testKeywordOnConstructorCompletion() = checkCompletion(
    hashSetOf("payable ", "internal ", "public "), """
      contract A{
        constructor()/*caret*/
      }
  """
  )

  fun testKeywordOnEventCompletion() = checkCompletion(
    hashSetOf("indexed "), """
      event /*caret*/
      
  """
  )

  fun testKeywordOnUsingCompletion() = checkCompletion(
    hashSetOf("for "), """
      using test /*caret*/
      
  """
  )

  fun testKeywordOnUsingForCompletion() = checkCompletion(
    hashSetOf("global "), """
      using test for uint256 /*caret*/
      
  """
  )

  fun testKeywordOnModifierCompletion() = checkCompletion(
    hashSetOf("override ", "virtual "), """
      contract A{
        modifier test() /*caret*/
       
      }
  """
  )

  fun testKeywordOnFallbackCompletion() = checkCompletion(
    hashSetOf("external ", "virtual ", "override ", "pure ", "view ", "payable "), """
      contract A{
        fallback() /*caret*/
       
      }
  """
  )

  fun testKeywordOnReceiveCompletion() = checkCompletion(
    hashSetOf("external ", "virtual ", "override ", "payable "), """
      contract A{
        receive() /*caret*/
       
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
