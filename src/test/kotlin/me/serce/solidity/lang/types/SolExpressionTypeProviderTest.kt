package me.serce.solidity.lang.types

import me.serce.solidity.lang.psi.SolExpression
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolExpressionTypeProviderTest : SolTestBase() {
  fun checkPrimitiveTypes(inference: Boolean = false, @Language("Solidity") codeProvider: (String, String) -> String) {
    var cases = listOf(
      "true" to "bool",
      "42" to "int256",
      "\"hello\"" to "string"
    )

    if(!inference) {
      cases += listOf(
        "42" to "uint8",
        "42" to "address",
        "100000" to "uint256"
      )
    }

    for ((value, type) in cases) {
      val code = codeProvider(value, type)
      checkExpr(code, "$value: $type")
    }
  }


  fun testAssignTyped() = checkPrimitiveTypes { value, type ->
    """
      contract A {
          function f() {
              $type x = $value;
              x;
            //^ $type
          }
      }
    """
  }

  fun testBoolVar() = checkPrimitiveTypes(true) { value, type ->
    """
        contract A {
            function f() {
                var x = $value;
                x;
              //^ $type
            }
        }
    """
  }

  fun testFunctionParameter() = checkPrimitiveTypes { value, type ->
    """
        contract A {
            function f($type x) {
                x;
              //^ $type
            }
        }
    """
  }


  fun testStateVar() = checkPrimitiveTypes { value, type ->
    """
      contract A {
          $type x;

          function f() {
              x;
            //^ $type
          }
      }
    """
  }

  fun testBoolTernary() = checkPrimitiveTypes(true) { value, type ->
    """
      contract A {
          function f() {
              var x = true ? $value : $value;
              x;
            //^ $type
          }
      }
    """
  }

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

    for ((value, type) in cases) {
      checkExpr("""
        contract A {
            function f() {
                var x = $value;
                x;
              //^ $type
            }
        }
    """, "$value: $type")
    }
  }

  private fun checkExpr(@Language("Solidity") code: String, msg: String = "") {
    InlineFile(code)
    val (expr, expectedType) = findElementAndDataInEditor<SolExpression>()
    assertEquals(msg, expectedType, expr.type.toString())
  }
}
