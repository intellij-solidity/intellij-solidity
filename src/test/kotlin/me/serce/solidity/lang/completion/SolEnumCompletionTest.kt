package me.serce.solidity.lang.completion

class SolEnumCompletionTest : SolCompletionTestBase() {

  fun testEnumAsFunctionParameter() = checkCompletion(hashSetOf("B1", "B2"), """
     contract A {
        enum B {B1, B2}

        function f_enum_param(B b){}

        function func_inv() {
            f_enum_param(B./*caret*/);
        }
    }
  """)

}
