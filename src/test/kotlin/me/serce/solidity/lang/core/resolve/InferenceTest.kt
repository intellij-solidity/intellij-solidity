package me.serce.solidity.lang.core.resolve

import junit.framework.TestCase
import me.serce.solidity.lang.psi.SolExpression
import me.serce.solidity.lang.psi.SolStructDefinition
import me.serce.solidity.lang.types.SolStruct
import me.serce.solidity.lang.types.SolType
import me.serce.solidity.lang.types.type
import org.intellij.lang.annotations.Language

class InferenceTest : SolResolveTestBase() {

  fun testStorageStruct() {
    InlineFile(
      code = """
         contract StructBase {
           struct Struct {}
                  //x
         }
      """,
      name = "base.sol"
    )

    val (struct, _) = findElementAndDataInEditor<SolStructDefinition>("x")
    checkType(SolStruct(struct), """
        import './base.sol';
        contract Contract is StructBase {
            function test() {
                Struct storage s;
                var test = s;
                         //^
            }
        }""")
  }

  private fun checkType(type: SolType, @Language("Solidity") code: String) {
    val (refElement, data) = resolveInCode<SolExpression>(code)
    TestCase.assertEquals(type, refElement.type)
  }
}
