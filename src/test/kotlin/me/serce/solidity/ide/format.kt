package me.serce.solidity.ide

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager

import java.io.File

import me.serce.solidity.utils.SolidityLightPlatformCodeInsightFixtureTestCase

class SolidityFormattingTest : SolidityLightPlatformCodeInsightFixtureTestCase() {
  private fun doTest() {
    val inputFile = this.inputFileName
    val inputText = FileUtil.loadFile(File(this.testDataPath + inputFile))
    this.myFixture.configureByText(inputFile, inputText)
    WriteCommandAction.runWriteCommandAction(this.myFixture.project, Runnable { CodeStyleManager.getInstance(this@SolidityFormattingTest.project).reformat(this@SolidityFormattingTest.myFixture.file as PsiElement) })
    val outputFile = File(this.myFixture.testDataPath + "/" + this.expectedOutputFileName)
    val expectedResultText = FileUtil.loadFile(outputFile, true)
    this.myFixture.checkResult(expectedResultText)
  }

  private val inputFileName: String
    get() = this.getTestName(true) + ".sol"

  private val expectedOutputFileName: String
    get() = this.getTestName(true) + "-after.sol"

  fun testContract() = this.doTest()
  fun testLibrary() = this.doTest()
  fun testIndent() = this.doTest()
  fun testStatementLineBreak() = this.doTest()

  override fun getTestDataPath(): String {
    return "src/test/resources/fixtures/formatter/"
  }

}
