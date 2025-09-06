package me.serce.solidity.ide.formatting

import com.intellij.lang.LanguageFormattingRestriction
import com.intellij.psi.PsiElement
import me.serce.solidity.lang.core.SolidityFile
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings

class SolidityFormattingRestriction : LanguageFormattingRestriction {

  override fun isFormatterAllowed(element: PsiElement): Boolean {
    // ensure only Solidity files are restricted by the formatter settings
    val file = element.containingFile
    if (file !is SolidityFile) {
      return true
    }

    val settings = SoliditySettings.getInstance(element.project)
    return settings.formatterType == FormatterType.INTELLIJ_SOLIDITY
  }
}
