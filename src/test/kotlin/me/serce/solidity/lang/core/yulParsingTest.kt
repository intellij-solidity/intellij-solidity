package me.serce.solidity.lang.core

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.ParsingTestCase
import me.serce.solidity.lang.SolidityLanguage

abstract class YulParsingTestBase(baseDir: String) : ParsingTestCase(baseDir, "yul", true, YulParserDefinition()) {
  private val solidityParserDefinition = SolidityParserDefinition()

  override fun setUp() {
    super.setUp()
    CoreApplicationEnvironment.registerExtensionPoint(
      Extensions.getRootArea(), "com.intellij.lang.braceMatcher", LanguageExtensionPoint::class.java)
    LanguageParserDefinitions.INSTANCE.addExplicitExtension(SolidityLanguage, solidityParserDefinition)
  }

  override fun tearDown() {
    LanguageParserDefinitions.INSTANCE.removeExplicitExtension(SolidityLanguage, solidityParserDefinition)
    super.tearDown()
  }

  override fun getTestDataPath() = "src/test/resources"
}

class YulParsingTest : YulParsingTestBase("fixtures/parser/yul") {
  fun testPower() = doTest(true, true)
  fun testObject() = doTest(true, true)
}
