package me.serce.solidity.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import me.serce.solidity.lang.core.SolidityTokenTypes.*

class SolBraceMatcher : PairedBraceMatcher {

  override fun getPairs() = PAIRS

  override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

  override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int) = openingBraceOffset

  companion object {
    private val PAIRS = arrayOf(
      BracePair(LBRACE, RBRACE, false),
      BracePair(LBRACKET, RBRACKET, false),
      BracePair(LPAREN, RPAREN, false)
    )
  }
}
