package me.serce.solidity.lang.completion

class SolFunctionNamesCompletionTest : SolCompletionTestBase() {

  fun testFuncNameInsideFunctionBlock() = checkCompletion(hashSetOf("f_1", "f_2"), """
    contract A {
      function f_1() {}
      function f_2() {}

      function example() {
        f_/*caret*/
      }
    }
  """, true)

  fun testFuncNameInsideIf() = checkCompletion(hashSetOf("f_1", "f_2"), """
    contract A {
      function f_1() {}
      function f_2() {}

      function example() {
        if(a > b) {
          f_/*caret*/
        }
      }
    }
  """, true)

  fun testFuncNameInsideAssignment() = checkCompletion(hashSetOf("f_1", "f_2"), """
    contract A {
      function f_1() {}
      function f_2() {}

      function example() {
        address a_1 = f_/*caret*/
      }
    }
  """, true)

  fun testFuncNameWithInheritance() = checkCompletion(hashSetOf("f_1", "f_2"), """
    contract FunctionHolder {
      function f_1() {}
      function f_2() {}
    }

    contract A is FunctionHolder {
      function example() {
        address a_1 = f_/*caret*/
      }
    }
  """, true)
}
