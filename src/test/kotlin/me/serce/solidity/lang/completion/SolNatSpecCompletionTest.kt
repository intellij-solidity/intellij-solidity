package me.serce.solidity.lang.completion

class SolNatSpecCompletionTest : SolCompletionTestBase() {
  fun testNatSpecCommentCompletion() = checkCompletion(hashSetOf("@author", "@title", "@return", "@notice"), """
        /// /*caret*/
        contract A {}
  """, strict = false)

  fun testNatSpecTagCompletion() = checkCompletion(hashSetOf("author"), """
        /**
         * @/*caret*/
         */ 
        contract A {}
  """, strict = false)
}
