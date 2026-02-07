package me.serce.solidity.ide.quickFix

import me.serce.solidity.ide.inspections.ResolveNameInspection

class SolImportFileTest : SolQuickFixTestBase() {
  fun testImportFileFix() {
    myFixture.enableInspections(ResolveNameInspection().javaClass)

    InlineFile(
      code = "contract a {}",
      name = "a.sol"
    )

    checkQuickFix(
      "contract b is a {}",
      "\nimport \"./a.sol\";contract b is a {}"
    )
  }

  fun testImportLibraryFromVarLiteral() {
    myFixture.enableInspections(ResolveNameInspection().javaClass)

    InlineFile(
      code = """
        library Math {
          function max(uint256 a, uint256 b) internal pure returns (uint256) {
            return a > b ? a : b;
          }
        }
      """,
      name = "Math.sol"
    )

    val mainFile = """
      contract C {
        function f(uint256 a, uint256 b) public pure returns (uint256) {
          return Math.max(a, b);
        }
      }
      """.trimIndent()
    checkQuickFix(mainFile, "\nimport \"./Math.sol\";$mainFile")
  }

  // https://github.com/intellij-solidity/intellij-solidity/issues/64
  fun testNoImportFixPopup() {
    myFixture.enableInspections(ResolveNameInspection().javaClass)

    InlineFile(
      code = """
        contract A {
          struct MyStruct {}
        }
      """,
      name = "A.sol"
    )

    InlineFile(
      code = """
        import "A.sol";

        contract B is A {
        }
      """,
      name = "B.sol"
    )

    assertNoQuickFix("""
      import "B.sol";

      contract C is B {
         MyStruct A; //My struct is correctly imported as its part of B
      }
    """
    )
  }

}
