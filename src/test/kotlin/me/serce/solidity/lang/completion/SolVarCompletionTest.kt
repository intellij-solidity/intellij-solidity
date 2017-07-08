package me.serce.solidity.lang.completion

class SolVarCompletionTest : SolCompletionTestBase() {
  fun testStateVarCompletion() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            address owner1;
            address owner2;

            function doit() {
                myNewOwner = ow/*caret*/;
            }
        }
  """)

  fun testGlobalVarCompletionTest() = checkCompletion(hashSetOf("msg", "now", "tx", "block"), """
        contract B {

            function doit() {
                var var1 = /*caret*/;
            }
        }
  """)

  fun testBlockCompletionTest() = checkCompletion(hashSetOf("coinbase", "difficulty"), """
        contract B {

            function doit() {
                var var1 = block./*caret*/;
            }
        }
  """)
}
