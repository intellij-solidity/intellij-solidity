package me.serce.solidity.lang.types

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import me.serce.solidity.lang.psi.SolFunctionCallExpression
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.utils.SolTestBase

class SolErc7201Test : SolTestBase() {
  fun testBuiltinInStorageLayoutAndArrayLength() {
    val file = InlineFile("""
      pragma solidity ^0.8.35;

      contract C layout at erc7201("namespace") {
          uint[erc7201("namespace")] values;
      }
    """).psiFile

    assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty())

    val calls = PsiTreeUtil.findChildrenOfType(file, SolFunctionCallExpression::class.java)
      .filter { it.expression.text == "erc7201" }
    assertEquals(2, calls.size)
    for (call in calls) {
      val builtin = call.reference?.resolve()
      assertTrue(builtin is SolFunctionDefinition)
      assertEquals("erc7201", (builtin as SolFunctionDefinition).name)
      assertEquals("uint256", call.type.toString())
    }
  }
}
