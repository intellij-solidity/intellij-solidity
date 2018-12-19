package me.serce.solidity.lang.types

import me.serce.solidity.lang.psi.SolExpression
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolExpressionTypeProviderTest : SolTestBase() {
  private fun checkPrimitiveTypes(inference: Boolean = false, @Language("Solidity") codeProvider: (String, String) -> String) {
    var cases = listOf(
      "true" to "bool",
      "1000" to "uint16",
      "\"hello\"" to "string"
    )

    if (!inference) {
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

  fun testFunctionParameter() = checkPrimitiveTypes { _, type ->
    """
        contract A {
            function f($type x) {
                x;
              //^ $type
            }
        }
    """
  }

  fun testStateVar() = checkPrimitiveTypes { _, type ->
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

  fun testContractTypes(@Language("Solidity") codeProvider: (String, String) -> String) {
    val cases = listOf(
      "B" to "B"
    )

    for ((name, type) in cases) {
      val code = codeProvider(name, type)
      checkExpr(code, "$name: $type")
    }
  }

  fun testContractParameter() = testContractTypes { _, type ->
    """
        contract B {}
        contract A {
            function f($type x) {
                x;
              //^ $type
            }
        }
    """
  }

  fun testContractStateVar() = testContractTypes { _, type ->
    """
        contract B {}
        contract A {
            B x;
            function f() {
                x;
              //^ $type
            }
        }
    """
  }

  fun testStructs() {
    checkExpr("""
        contract A {
            struct B {}
            B b;
            function f() {
              var x = b;
              x;
            //^ B
            }
        }
    """)
  }

  fun testArrays() {
    val cases = listOf(
      ("d" to "int[]") to "int256[]",
      ("d" to "int[5]") to "int256[5]",
      ("d[0]" to "int256[]") to "int256",
      ("d" to "mapping(int8 => A)") to "mapping(int8 => A)",
      ("d[1]" to "mapping(int8 => A)") to "A"
    )

    for ((value, type) in cases) {
      checkExpr("""
        contract A {
            ${value.second} d;

            function f() {
              var x = ${value.first};
              x;
            //^ $type
            }
        }
      """)
    }
  }

  fun testThisType() {
    checkExpr("""
         contract A {
            function f() {
                this;
              //^ A
            }
        }
    """)
  }

  fun testMessage() {
    checkExpr("""
         contract A {
            function f() {
                msg;
               //^ Msg
            }
        }
    """)
  }

  fun testMessageProperty() {
    checkExpr("""
         contract A {
            function f() {
                msg.sender;
                    //^ address
            }
        }
    """)
  }

  fun testBlock() {
    checkExpr("""
         contract A {
            function f() {
                block;
               //^ Block
            }
        }
    """)
  }

  fun testBlockStateVar() {
    checkExpr("""
         contract A {
            function f() {
                block.coinbase;
                        //^ address
            }
        }
    """)
  }

  fun testTx() {
    checkExpr("""
         contract A {
            function f() {
                tx;
               //^ Tx
            }
        }
    """)
  }

  fun testEnum() {
    checkExpr("""
        contract A {
            enum B { A1, A2 }
            function f() {
                B.A1;
                //^ B
            }
        }
    """)
  }

  fun testIndexAccessType() {
    checkExpr(    """
      contract A {
          struct S { int b; }
          function f(S[] arr) {
              var x = 1 == arr[1].b;
              x;
            //^ bool
          }
      }
    """)
  }

  private fun checkExpr(@Language("Solidity") code: String, msg: String = "") {
    InlineFile(code)
    val (expr, expectedType) = findElementAndDataInEditor<SolExpression>()
    assertEquals(msg, expectedType, deInternalise(expr.type.toString()))
  }
}
