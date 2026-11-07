package me.serce.solidity.ide.formatting

import com.intellij.lang.LanguageFormattingRestriction
import com.intellij.openapi.project.Project
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

    return isIdeFormatterActive(element.project)
  }

  companion object {
    /** False when an external formatter (Foundry) or none is configured; edits must then be formatted by hand. */
    fun isIdeFormatterActive(project: Project): Boolean =
      SoliditySettings.getInstance(project).formatterType == FormatterType.INTELLIJ_SOLIDITY
  }
}
