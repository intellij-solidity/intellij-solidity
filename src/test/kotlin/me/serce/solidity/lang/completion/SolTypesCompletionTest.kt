package me.serce.solidity.lang.completion

class SolTypesCompletionTest : SolCompletionTestBase() {

  fun testStateVarsTypeCompletions() = checkCompletion(
    hashSetOf(
      "address ",
      "uint8 ",
      "uint16 ",
      "uint32 ",
      "uint64 ",
      "uint128 ",
      "uint256 ",
      "int8 ",
      "int16 ",
      "int32 ",
      "int64 ",
      "int128 ",
      "int256 ",
    ), """
     contract A {
      /*caret*/
     }
  """
  )

  fun testBaseTypesSubsetCompletion() = checkCompletion(
    hashSetOf("bytes ", "bytes4 ", "bytes8 ", "bytes16 ", "bytes20 ", "bytes32 ", "byte ", "bool "), """
     contract A {
      b/*caret*/
     }
  """
  )

}
