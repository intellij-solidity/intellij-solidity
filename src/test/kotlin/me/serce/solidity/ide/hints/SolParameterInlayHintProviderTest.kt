package me.serce.solidity.ide.hints

import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolFunctionCallArguments
import me.serce.solidity.utils.SolTestBase

class SolParameterInlayHintProviderTest : SolTestBase() {
  private val provider = SolParameterInlayHintProvider()

  fun testNamedFunctionArgumentsDoNotGetHints() {
    val file = myFixture.configureByText("namedArguments.sol", """
      contract Example {
        function swap(uint amount, bool zeroForOne) internal {}

        function callSwap() internal {
          swap(1, true);
          swap({zeroForOne: true, amount: 1});
        }
      }
    """.trimIndent())

    val arguments = PsiTreeUtil.findChildrenOfType(file, SolFunctionCallArguments::class.java).toList()
    assertEquals(listOf("amount", "zeroForOne"), provider.getParameterHints(arguments[0]).map { it.text })
    assertTrue(provider.getParameterHints(arguments[1]).isEmpty())
  }

  fun testNamedStructArgumentsDoNotSuppressOuterPositionalHints() {
    val file = myFixture.configureByText("nestedNamedArguments.sol", """
      contract Example {
        struct Config {
          uint amount;
          bool enabled;
        }

        function useConfig(Config memory config, uint nonce) internal {}

        function run() internal {
          useConfig(Config({enabled: true, amount: 1}), 7);
        }
      }
    """.trimIndent())

    val arguments = PsiTreeUtil.findChildrenOfType(file, SolFunctionCallArguments::class.java).toList()
    assertEquals(listOf("config", "nonce"), provider.getParameterHints(arguments[0]).map { it.text })
    assertTrue(provider.getParameterHints(arguments[1]).isEmpty())
  }

  fun testCallOptionsDoNotSuppressPositionalHints() {
    val file = myFixture.configureByText("callOptions.sol", """
      contract Example {
        function swap(uint amount, bool zeroForOne) external {}

        function callSwap() external {
          this.swap{gas: 30000}(1, true);
          this.swap{gas: 30000}({zeroForOne: true, amount: 1});
        }
      }
    """.trimIndent())

    val arguments = PsiTreeUtil.findChildrenOfType(file, SolFunctionCallArguments::class.java).toList()
    assertEquals(listOf("amount", "zeroForOne"), provider.getParameterHints(arguments[0]).map { it.text })
    assertTrue(provider.getParameterHints(arguments[1]).isEmpty())
  }
}
