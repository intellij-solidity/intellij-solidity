package me.serce.solidity.lang.completion

class SolConstantVariableCompletionTest : SolCompletionTestBase() {
  fun testConstantVariableCompletion() = checkCompletion(hashSetOf("contractOwner", "contractAuthor"), """
        uint constant contractOwner = 0x123;
        uint constant contractAuthor = 0x456;
        
        contract B {
            function doit() {
                uint addr = con/*caret*/;
            }
        }
  """, strict = true)
}
