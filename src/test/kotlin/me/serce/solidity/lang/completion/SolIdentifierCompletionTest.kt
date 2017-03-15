package me.serce.solidity.lang.completion

class SolIdentifierCompletionTest : SolCompletionTestBase() {
  fun testVariableCompletion() = checkCompletion(hashSetOf("Foo", "FooBar"), """
    contract Foo {}
    contract FooBar {}

    contract A {
        function f() {
            Fo/*caret*/ kk;
        }
    }

  """)
}
