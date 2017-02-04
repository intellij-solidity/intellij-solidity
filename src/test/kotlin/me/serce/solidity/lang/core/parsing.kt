/**
 * Created by serce on 29/1/17.
 */
package me.serce.solidity.lang.core

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.testFramework.ParsingTestCase

abstract class SolidityParsingTestBase : ParsingTestCase("fixtures/parser", "sol", true, SolidityParserDefinition()) {

  override fun setUp() {
    super.setUp()
    CoreApplicationEnvironment.registerExtensionPoint(
      Extensions.getRootArea(), "com.intellij.lang.braceMatcher", LanguageExtensionPoint::class.java)
  }

  override fun getTestDataPath() = "src/test/resources"
}

class SolidityCompleteParsingTest : SolidityParsingTestBase() {
  fun testPragma() = doTest(true)
  fun testComments() = doTest(true)
  fun testImports() = doTest(true)
  fun testMappings() = doTest(true)
  fun testEvent() = doTest(true)
  fun testFunctions() = doTest(true)
  fun testMapInvoke() = doTest(true)
  fun testDestructuring() = doTest(true)
  fun testContractWithLiterals() = doTest(true)
  fun testContractWithDifferentFields() = doTest(true)
}
