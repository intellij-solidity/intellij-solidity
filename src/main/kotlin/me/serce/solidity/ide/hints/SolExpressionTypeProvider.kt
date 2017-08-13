package me.serce.solidity.ide.hints

import com.intellij.lang.ExpressionTypeProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import me.serce.solidity.lang.psi.SolExpression
import me.serce.solidity.lang.types.type

class SolExpressionTypeProvider : ExpressionTypeProvider<SolExpression>() {
  override fun getExpressionsAt(pivot: PsiElement): MutableList<SolExpression> {
    return SyntaxTraverser.psiApi().parents(pivot)
      .filter(SolExpression::class.java)
      .toList()
  }

  override fun getInformationHint(element: SolExpression): String = StringUtil.escapeXml(element.type.toString())

  override fun getErrorHint() = "Select an expression"
}
