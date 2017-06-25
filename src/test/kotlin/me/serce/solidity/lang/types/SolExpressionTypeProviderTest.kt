package me.serce.solidity.lang.types

import me.serce.solidity.lang.psi.SolExpression
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolExpressionTypeProviderTest : SolTestBase() {
  fun testBoolTyped() = checkExpr("""
    contract A {
        function f() {
            bool x = true;
            x;
          //^ bool
        }
    }
  """)

  fun testBoolVar() = checkExpr("""
    contract A {
        function f() {
            var x = true;
            x;
          //^ bool
        }
    }
  """)

  fun testFunctionParameter() = checkExpr("""
    contract A {
        function f(bool x) {
            x;
          //^ bool
        }
    }
  """)


  fun testStateVar() = checkExpr("""
    contract A {
        bool x;

        function f() {
            x;
          //^ bool
        }
    }
  """)

  fun testBoolTernary() = checkExpr("""
    contract A {
        function f() {
            var x = true ? true : false;
            x;
          //^ bool
        }
    }
  """)

  fun testBinOperatorsBool() {
    val cases = listOf(
      Pair("1 == 2", "bool"),
      Pair("1 != 2", "bool"),
      Pair("1 <= 2", "bool"),
      Pair("1 >= 2", "bool"),
      Pair("1 < 2", "bool"),
      Pair("1 > 2", "bool"),
      Pair("true && false", "bool"),
      Pair("true || false", "bool")
    )

    for (case in cases) {
      checkExpr("""
        contract A {
            function f() {
                var x = ${case.first};
                x;
              //^ ${case.second}
            }
        }
    """, case.toString())
    }
  }

  private fun checkExpr(@Language("Solidity") code: String, msg: String = "") {
    InlineFile(code)
    val (expr, expectedType) = findElementAndDataInEditor<SolExpression>()
    assertEquals(msg, expectedType, expr.type.toString())
  }
}
