package me.serce.solidity.ide.quickFix

class SolImportFileTest: SolQuickFixTestBase() {
  fun testImportFileFix() {
    InlineFile(
      name = "a.sol",
      code = "contract a {}"
    )

    testQuickFix(
      "contract b is a {}",
      "\nimport \"./a.sol\";contract b is a {}"
    )
  }
}
