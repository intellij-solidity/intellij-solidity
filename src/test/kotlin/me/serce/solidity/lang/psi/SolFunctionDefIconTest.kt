package me.serce.solidity.lang.psi

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.icons.RowIcon
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertSame

class SolFunctionDefIconTest : SolTestBase() {
  fun testVisibilityAndMutabilityIcons() {
    InlineFile(
      """
      contract C {
          function defFunc() {}
          function pubFunc() public {}
          function privView() private view {}
          function intPure() internal pure {}
          function extPay() external payable {}
      }
      """
    )

    val functions = PsiTreeUtil.collectElementsOfType(myFixture.file, SolFunctionDefinition::class.java)
      .associateBy { it.name }

    val defIcon = functions["defFunc"]?.getIcon(0) as? RowIcon
    assertSame(SolidityIcons.FUNCTION, defIcon?.getIcon(0))
    assertSame(SolidityIcons.WRITE, defIcon?.getIcon(1))

    val pubIcon = functions["pubFunc"]?.getIcon(0) as? RowIcon
    assertSame(SolidityIcons.FUNCTION_PUB, pubIcon?.getIcon(0))
    assertSame(SolidityIcons.WRITE, pubIcon?.getIcon(1))

    val privIcon = functions["privView"]?.getIcon(0) as? RowIcon
    assertSame(SolidityIcons.FUNCTION_PRV, privIcon?.getIcon(0))
    assertSame(SolidityIcons.VIEW, privIcon?.getIcon(1))

    val intIcon = functions["intPure"]?.getIcon(0) as? RowIcon
    assertSame(SolidityIcons.FUNCTION_INT, intIcon?.getIcon(0))
    assertSame(SolidityIcons.PURE, intIcon?.getIcon(1))

    val extIcon = functions["extPay"]?.getIcon(0) as? RowIcon
    assertSame(SolidityIcons.FUNCTION_EXT, extIcon?.getIcon(0))
    assertSame(SolidityIcons.PAYABLE, extIcon?.getIcon(1))
  }

  fun testReceiveFunctionIcon() {
    InlineFile(
      """
      contract C {
          receive() external payable {}
      }
      """
    )

    val function = PsiTreeUtil.collectElementsOfType(myFixture.file, SolFunctionDefinition::class.java).single()
    val icon = function.getIcon(0)
    assertSame(SolidityIcons.RECEIVE, icon)
  }
}
