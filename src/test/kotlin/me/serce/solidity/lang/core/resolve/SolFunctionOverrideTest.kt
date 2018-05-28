package me.serce.solidity.lang.core.resolve

import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.resolve.function.SolFunctionResolver

class SolFunctionOverrideTest : SolResolveTestBase() {
  fun testFindOverrides() {
    InlineFile("""
        contract A {
            function test(uint256 test);
                     //X
            function test();
        }

        contract B is A {
            function test(uint test1) {
                     //^

            }

            function test(uint128 test2) {

            }

        }

        contract C is B {
            function test(uint256 test2) {
                     //Y
            }

            function test(uint128 test) {

            }
        }
  """)
    val (func, _) = findElementAndDataInEditor<SolFunctionDefinition>("^")
    val (overriden, _) = findElementAndDataInEditor<SolFunctionDefinition>("X")
    val (overrides, _) = findElementAndDataInEditor<SolFunctionDefinition>("Y")

    val overridenList = SolFunctionResolver.collectOverriden(func)
    assert(overridenList.size == 1)
    assert(overridenList.firstOrNull() == overriden)

    val overridesList = SolFunctionResolver.collectOverrides(func)
    assert(overridesList.size == 1)
    assert(overridesList.firstOrNull() == overrides)
  }
}
