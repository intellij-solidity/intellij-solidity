package me.serce.solidity.ide.formatting

class SolAutoIdentationTest: SolTypingTestBase() {
  override fun getTestDataPath() = "src/test/resources/fixtures/auto_ident/"

  fun testAssembly() = doTest()
  fun testContract() = doTest()
  fun testEnum() = doTest()
  fun testStruct() = doTest()
  fun testFunction() = doTest()
  fun testIf() = doTest()
  fun testStateVar() = doTest()
  fun testPragma() = doTest()
}
