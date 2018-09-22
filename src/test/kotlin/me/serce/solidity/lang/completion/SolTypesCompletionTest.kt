package me.serce.solidity.lang.completion

class SolTypesCompletionTest : SolCompletionTestBase() {

  fun testStateVarsTypeCompletions() = checkCompletion(hashSetOf("address ", "uint ", "int "), """
     contract A {
      /*caret*/
     }
  """)

  fun testBaseTypesSubsetCompletion() = checkCompletion(hashSetOf("bytes ", "byte ", "bool "), """
     contract A {
      b/*caret*/
     }
  """)

}
