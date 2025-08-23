package me.serce.solidity.lang.completion

class SolTypesCompletionTest : SolCompletionTestBase() {

  fun testStateVarsTypeCompletions() = checkCompletion(
    elementaryType, """
     contract A {
      /*caret*/
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

  fun testMappingValueTypeCompletion() = checkCompletion(
    elementaryType, """
      contract A {
      mapping(address =>/*caret*/
     }
       
  """
  )

  fun testFunctionTypeCompletions() = checkCompletion(
    elementaryType + hashSetOf("mapping"), """
     contract A {
      function b(/*caret*/
     }
  """
  )

  fun testInsideFunctionTypeCompletions() = checkCompletion(
    elementaryType, """
     contract A {
      function b() {
      /*caret*/
      }
     }
  """
  )

}
