package me.serce.solidity.lang.completion

class SolTypesCompletionTest : SolCompletionTestBase() {

  fun testStateVarsTypeCompletions() = checkCompletion(
    elementaryType, """
     contract A {
      /*caret*/
     }
  """
  )

  fun testPayableAddressTypeCompletions() = checkCompletion(
    hashSetOf("payable"), """
     contract A {
      address /*caret*/
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

  fun testFunctionTypeCompletions() = checkCompletion(
    elementaryType + hashSetOf("mapping"), """
     contract A {
      function b(/*caret*/
     }
  """
  )

}
