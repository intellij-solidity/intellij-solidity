package me.serce.solidity.lang.completion

class SolTypesCompletionTest : SolCompletionTestBase() {

  fun testStateVarsTypeCompletions() = checkCompletion(
    elementaryType, """
     contract A {
      /*caret*/
     }
  """
  )

  fun testBaseTypesSubsetCompletion() = checkCompletion(
    elementaryTypeB, """
     contract A {
      b/*caret*/
     }
  """
  )

  fun testUserDefinedValueTypeCompletion() = checkCompletion(
    elementaryType, """
      type Test is /*caret*/
  """
  )

  fun testMappingKeyTypeCompletion() = checkCompletion(
    elementaryType, """
      contract A {
      mapping(/*caret*/
     }
       
  """
  )

}
