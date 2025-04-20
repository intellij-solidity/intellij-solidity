package me.serce.solidity.lang.completion

class SolNatSpecCompletionTest : SolCompletionTestBase() {
  fun testNatSpecCommentCompletion() = checkCompletion(
    hashSetOf("@author", "@title", "@dev", "@param", "@inheritdoc", "@custom", "@return", "@notice"), """
        /// /*caret*/
        contract A {}
  """, strict = false
  )

  fun testNatSpecTagCompletion() = checkCompletion(
    hashSetOf("author", "title", "dev", "param", "inheritdoc", "custom", "return", "notice"), """
        /**
         * @/*caret*/
         */ 
        contract A {}
  """, strict = false
  )
}
