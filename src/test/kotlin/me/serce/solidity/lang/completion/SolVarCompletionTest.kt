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

  fun testStateVarCompletionIncomplete() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            address owner1;
            address owner2;

            function doit() {
                own/*caret*/
            }
        }
  """)

  fun testGlobalVarCompletionTest() = checkCompletion(hashSetOf("msg", "tx", "block"), """
        contract B {

            function doit() {
                var var1 = /*caret*/;
            }
        }
  """)

  fun testVarCompletionTestIncomplete() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            address owner1;
            address owner2;

            function doit() {
                var var1 = own/*caret*/
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

  fun testArrayIndexCompletionTest1() = checkCompletion(
    hashSetOf("userIndex"), """
        contract B {
            address[] users;

            function doit() {
                uint userIndex = 0;
                users[/*caret*/];
            }
        }
  """
  )

  fun testArrayIndexCompletionTest2() = checkCompletion(
    hashSetOf("userIndex"), """
        contract B {
            address[] users;

            function doit() {
                uint userIndex = 0;
                users[/*caret*/]
            }
        }
  """
  )

  fun testArrayAssignment() = checkCompletion(
    hashSetOf("A"), """
        contract B {
            address[] users;
            struct A {
                uint a1;
            }

            function doit() {
                users[userIndex] = /*caret*/;
            }
        }
  """
  )
}
