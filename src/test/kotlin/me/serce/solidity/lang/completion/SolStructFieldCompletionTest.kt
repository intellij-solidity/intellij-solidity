package me.serce.solidity.lang.completion

/**
 * TODO: incomplete test (without ;)
 */
class SolStructFieldCompletionTest : SolCompletionTestBase() {
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
}
