package me.serce.solidity.ide.formatting

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings

class SolidityFormattingRestrictionTest : BasePlatformTestCase() {
  // ensure the changes in settings don't leak beyond the current test case. 
  private lateinit var originalFormatterType: FormatterType

  override fun setUp() {
    super.setUp()
    originalFormatterType = SoliditySettings.getInstance(project).formatterType
  }

  override fun tearDown() = try {
    SoliditySettings.getInstance(project).formatterType = originalFormatterType
  } finally {
    super.tearDown()
  }

  fun testNonSolidityFilesAreAlwaysAllowed() {
    val psi = myFixture.configureByText("A.txt", "plain text")
    SoliditySettings.getInstance(project).apply {
      // any value here shouldn't change the behaviour
      formatterType = FormatterType.DISABLED
    }

    val restriction = SolidityFormattingRestriction()
    assertTrue(restriction.isFormatterAllowed(psi))
  }

  fun testSolidityAllowsIdeFormatterWhenIntellijFormatterSelected() {
    val psi = myFixture.configureByText("B.sol", "contract B { }")
    SoliditySettings.getInstance(project).apply {
      formatterType = FormatterType.INTELLIJ_SOLIDITY
    }

    val restriction = SolidityFormattingRestriction()
    assertTrue(restriction.isFormatterAllowed(psi))
  }

  fun testSolidityBlocksIdeFormatterWhenFoundryOrDisabledSelected() {
    val psi = myFixture.configureByText("C.sol", "contract C { }")
    val settings = SoliditySettings.getInstance(project)

    val restriction = SolidityFormattingRestriction()

    settings.formatterType = FormatterType.FOUNDRY
    assertFalse(restriction.isFormatterAllowed(psi))

    settings.formatterType = FormatterType.DISABLED
    assertFalse(restriction.isFormatterAllowed(psi))
  }
}
