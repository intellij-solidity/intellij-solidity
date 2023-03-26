package me.serce.solidity.lang.completion

class SolStateVarCompletionTest : SolCompletionTestBase() {
  fun testModifierCompletion() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            struct C {
                address owner1;
                address owner2;
            }
            C c;

            function doit() {
                c./*caret*/;
            }
        }
  """)

  fun testModifierCompletionMultiple() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract B {
            struct C {
                int nway;
                string prop;
                address owner1;
                address owner2;
            }
            C c;

            function doit() {
                c.ow/*caret*/;
            }
        }
  """, strict = true)

  fun testContractCompletion() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract C {
            address public owner1;
            address public owner2;
        }
        contract B {
            C c;

            function doit() {
                c.ow/*caret*/;
            }
        }
  """, strict = true)

  fun testContractCompletionInheritance() = checkCompletion(hashSetOf("owner1", "owner2"), """
        contract C {
            address public owner1;
            address public owner2;
        }
        contract D is C {}
        contract B {
            D c;

            function doit() {
                c.ow/*caret*/;
            }
        }
  """, strict = true)

  fun testBuiltinMemberAccess() = checkCompletion(hashSetOf("length", "push", "pop"), """
        contract B {
            int[] c;

            function doit() {
                c./*caret*/;
            }
        }
  """)
}
