package me.serce.solidity.ide.folding

import me.serce.solidity.utils.SolTestBase

class SolidityFoldingBuilderTest  : SolTestBase() {
  override fun getTestDataPath() = "src/test/resources/fixtures/folding/"

  fun testComments() = doTest()
  // TODO: fix natspec folding
// fun testNatSpec() = doTest()
  fun testElements() = doTest()

  private fun doTest() {
    myFixture.testFolding("${testDataPath}/$fileName")
  }
}
