package me.serce.solidity.ide

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.formatting.FormattingModelProvider
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.formatting.SpacingBuilder
import me.serce.solidity.lang.SolidityLanguage
import com.intellij.psi.PsiFile


class SolidityFormattingModelBuilder : FormattingModelBuilder {
  private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
    return SpacingBuilder(settings, SolidityLanguage)
  }

  override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
    val spacingBuilder = createSpacingBuilder(settings)

    val containingFile = element.containingFile
    val solidityBlock = SolidityBlock(element.node, spacingBuilder)

    return FormattingModelProvider.createFormattingModelForPsiFile(containingFile, solidityBlock, settings)
  }

  override fun getRangeAffectingIndent(file: PsiFile, offset: Int, elementAtOffset: ASTNode): TextRange? {
    return null
  }
}
