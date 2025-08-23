package me.serce.solidity.lang.completion

class SolFunctionsCompletionTest : SolCompletionTestBase() {

  fun testFuncNameIfConditionPrimExpression() = checkCompletion(
    hashSetOf("f_1", "f_2"), """
    contract FunctionHolder {
      function f_1() {}
      function f_2() {}
    }

    contract A is FunctionHolder {
      function example() {
        if(/*caret*/)
      }
    }
  """
  )

  fun testFuncNameIfConditionFunctionCall() = checkCompletion(
    hashSetOf("f_1", "f_2"), """
    contract FunctionHolder {
      function f_1() {}
      function f_2() {}
    }

    contract A is FunctionHolder {
      function example() {
        if(f_/*caret*/())
      }
    }

  """
  )

  fun testFuncNameWhileCond() = checkCompletion(
    hashSetOf("f_1", "f_2"), """
    contract FunctionHolder {
      function f_1() {}
      function f_2() {}
    }

    contract A is FunctionHolder {
      function example() {
        while(f_/*caret*/)
      }
    }
  """
  )

  fun testFuncNameReturn() = checkCompletion(
    hashSetOf("f_1", "f_2"), """
    contract FunctionHolder {
      function f_1() {}
      function f_2() {}
    }

    contract A is FunctionHolder {
      function example() {
        return /*caret*/
      }
    }
  """
  )

  fun testFuncArgumentsTest() {
    checkCompletion(
      hashSetOf("f_1", "f_2"), """
            contract FunctionHolder {
              function f_1() {}
              function f_2() {}
            }

            contract A is FunctionHolder {
              function example(uint amount, uint balance) {
                f_1(/*caret*/);
              }
            }
          """, strict = false
    )
    checkCompletion(
      hashSetOf("f_1", "f_2"), """
            contract FunctionHolder {
              function f_1() {}
              function f_2() {}
            }

            contract A is FunctionHolder {
              function example(uint amount, uint balance) {
                f_1(amount, /*caret*/);
              }
            }
          """, strict = false
    )
    checkCompletion(
      hashSetOf("f_1", "f_2"), """
            contract FunctionHolder {
              function f_1() {}
              function f_2() {}
            }

            contract A is FunctionHolder {
              function example(uint amount, uint balance) {
                f_1(/*caret*/, amount);
              }
            }
          """
    )

    checkCompletion(
      hashSetOf("param1", "param2"), """
            contract A {
              function example(uint param1, uint param2) {
                f_1(/*caret*/, amount);
              }
            }
          """
    )

    checkCompletion(
      hashSetOf("param1", "param2"), """
            contract A {
              function example(uint param1, uint param2) {
                f_1(amount, /*caret*/);
              }
            }
          """
    )
  }

  fun testFuncArgumentsTestIncompleteStatement() = checkCompletion(
    hashSetOf("param1", "param2"), """
            contract A {
              function example(uint param1, uint param2) {
                f_1(/*caret*/)
              }
            }
          """
  )
}
