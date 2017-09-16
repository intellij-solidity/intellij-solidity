package me.serce.solidity.ide.colors

import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import me.serce.solidity.ide.SolHighlighter
import me.serce.solidity.ide.SolidityIcons
import me.serce.solidity.lang.SolidityLanguage
import me.serce.solidity.loadCodeSampleResource

class SolColorSettingsPage : ColorSettingsPage {
  private val ATTRIBUTES: Array<AttributesDescriptor> = SolColor.values().map { it.attributesDescriptor }.toTypedArray()
  private val ANNOTATOR_TAGS = SolColor.values().associateBy({ it.name }, { it.textAttributesKey })

  private val DEMO_TEXT by lazy {
    loadCodeSampleResource(this, "me/serce/solidity/ide/colors/highlighter_example.sol")
  }

  override fun getDisplayName() = SolidityLanguage.displayName
  override fun getIcon() = SolidityIcons.FILE_ICON
  override fun getAttributeDescriptors() = ATTRIBUTES
  override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
  override fun getHighlighter() = SolHighlighter()
  override fun getAdditionalHighlightingTagToDescriptorMap() = ANNOTATOR_TAGS
  override fun getDemoText() = DEMO_TEXT
}
