package me.serce.solidity.lang.types

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolMemberAccessExpression
import me.serce.solidity.lang.psi.SolStateVariableDeclaration
import me.serce.solidity.lang.psi.SolYulFunctionCall
import me.serce.solidity.utils.SolTestBase

class SolSlotnumTest : SolTestBase() {
  fun testSolidityAndInlineAssembly() {
    val file = InlineFile("""
      pragma solidity ^0.8.37;

      contract C {
          function f() external view returns (uint64 result) {
              result = block.slotnum;
              assembly { let slot := slotnum() }
          }
      }
    """).psiFile

    assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
    val member = PsiTreeUtil.findChildrenOfType(file, SolMemberAccessExpression::class.java)
      .single { it.text == "block.slotnum" }
    assertEquals("slotnum", (member.reference?.resolve() as? SolStateVariableDeclaration)?.name)
    assertEquals("uint64", member.type.toString())
    assertEquals(1, PsiTreeUtil.findChildrenOfType(file, SolYulFunctionCall::class.java)
      .count { it.firstChild.text == "slotnum" })
  }

  fun testStandaloneYul() {
    val file = InlineFile("""
      object "Slot" {
          code { let slot := slotnum() }
      }
    """, "slotnum.yul").psiFile

    assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())
    assertEquals(1, PsiTreeUtil.findChildrenOfType(file, SolYulFunctionCall::class.java)
      .count { it.firstChild.text == "slotnum" })
  }
}
