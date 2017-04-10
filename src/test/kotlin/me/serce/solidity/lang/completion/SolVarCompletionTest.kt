package me.serce.solidity.lang.completion

class SolVarCompletionTest : SolCompletionTestBase() {
  fun testModifierCompletion() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            address owner1;
            address owner2;

            function doit() {
                myNewOwner = ow/*caret*/;
            }
        }
  """)
}
