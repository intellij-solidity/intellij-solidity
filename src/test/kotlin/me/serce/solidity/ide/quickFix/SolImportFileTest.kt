package me.serce.solidity.ide.quickFix

import me.serce.solidity.ide.inspections.ResolveNameInspection

class SolImportFileTest: SolQuickFixTestBase() {
  fun testImportFileFix() {
    myFixture.enableInspections(ResolveNameInspection().javaClass)

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
