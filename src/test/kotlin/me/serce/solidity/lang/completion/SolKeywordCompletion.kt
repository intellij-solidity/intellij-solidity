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

}
