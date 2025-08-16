package me.serce.solidity.ide.formatting

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import me.serce.solidity.utils.SolLightPlatformCodeInsightFixtureTestCase
import java.io.File

class SolidityFormattingTest : SolLightPlatformCodeInsightFixtureTestCase() {
  private fun doTest() {
    val inputFile = inputFileName
    val inputText = FileUtil.loadFile(File(testDataPath + inputFile))
    myFixture.configureByText(inputFile, inputText)
    WriteCommandAction.runWriteCommandAction(project) {
      CodeStyleManager.getInstance(project).reformat(myFixture.file as PsiElement)
    }
    myFixture.checkResultByFile(expectedOutputFileName)
  }

  private val inputFileName: String
    get() = this.getTestName(true) + ".sol"

  private val expectedOutputFileName: String
    get() = this.getTestName(true) + "-after.sol"

  fun testAssembly() = this.doTest()
  fun testInsideParens() = this.doTest()
  fun testIf() = this.doTest()
  fun testTernary() = this.doTest()
  fun testVarDefinition() = this.doTest()
  fun testLineComments() = this.doTest()
  fun testContract() = this.doTest()
  fun testVoting() = this.doTest()
  fun testLibrary() = this.doTest()
  fun testIndent() = this.doTest()
  fun testEvents() = this.doTest()
  fun testErrors() = this.doTest()
  fun testEnum() = this.doTest()
  fun testStatementLineBreak() = this.doTest()
  fun testStatementLineBreakAtEnd() = this.doTest()
  fun testBlankLinesBetweenContracts() = this.doTest()
  fun testBlankLinesBetweenContractParts() = this.doTest()
  fun testBlankLinesBetweenContractAndStruct() = this.doTest()
  fun testSpaceAfterReturns() = this.doTest()
  fun testMultisigWallet() = this.doTest()
  fun testInlineArray() = this.doTest()
  fun testInheritanceList() = this.doTest()

  override fun getTestDataPath() = "src/test/resources/fixtures/formatter/"
}
