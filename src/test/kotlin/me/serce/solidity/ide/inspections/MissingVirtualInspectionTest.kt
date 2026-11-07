package me.serce.solidity.ide.inspections

import me.serce.solidity.settings.FormatterType
import me.serce.solidity.settings.SoliditySettings
import org.intellij.lang.annotations.Language

class MissingVirtualInspectionTest : SolInspectionsTestBase(MissingVirtualInspection()) {

  fun testRemixExample() = checkByText("""
    contract SimpleStorage {
        uint256 myFavoriteNumber;
        function /*@error descr="Function is overridden but not marked virtual"@*/store/*@/error@*/(uint256 _favoriteNumber) public {
            myFavoriteNumber = _favoriteNumber;
        }
    }
    contract AddFiveStorage is SimpleStorage {
        function /*@error descr="Overridden function 'SimpleStorage.store' is not marked virtual"@*/store/*@/error@*/(uint256 _favoriteNumber) public {}
    }
  """)

  fun testVirtualOk() = checkByText("""
    contract A { function f() public virtual {} }
    contract B is A { function f() public override {} }
  """)

  fun testInterfaceOk() = checkByText("""
    interface I { function f() external; }
    contract C is I { function f() external {} }
  """)

  fun testAbstractBodylessNoError() = checkByText("""
    abstract contract A { function f() public virtual; }
    contract B is A { function f() public override {} }
  """)

  fun testNotOverriddenNoError() = checkByText("""
    contract A { function f() public {} }
    contract B is A { function g() public {} }
  """)

  fun testModifier() = checkByText("""
    contract A { modifier /*@error descr="Modifier is overridden but not marked virtual"@*/m/*@/error@*/() { _; } }
    contract B is A { modifier /*@error descr="Overridden modifier 'A.m' is not marked virtual"@*/m/*@/error@*/() override { _; } }
  """)

  fun testIntermediateOverrideMustBeVirtual() = checkByText("""
    contract A { function f() public virtual {} }
    contract B is A { function /*@error descr="Function is overridden but not marked virtual"@*/f/*@/error@*/() public override {} }
    contract C is B { function /*@error descr="Overridden function 'B.f' is not marked virtual"@*/f/*@/error@*/() public override {} }
  """)

  fun testFixAddVirtualOnBase() = checkFix(
    "Add virtual",
    """
    contract A { function <caret>f() public {} }
    contract B is A { function f() public override {} }
    """,
    """
    contract A { function f() public virtual {} }
    contract B is A { function f() public override {} }
    """
  )

  fun testFixAddVirtualBeforeOverride() = checkFix(
    "Add virtual",
    """
    contract A { function f() public virtual {} }
    contract B is A { function <caret>f() public override {} }
    contract C is B { function f() public override {} }
    """,
    """
    contract A { function f() public virtual {} }
    contract B is A { function f() public virtual override {} }
    contract C is B { function f() public override {} }
    """
  )

  fun testFixFromOverrideSite() = checkFix(
    "Make 'A.f' virtual",
    """
    contract A { function f() public {} }
    contract B is A { function <caret>f() public override {} }
    """,
    """
    contract A { function f() public virtual {} }
    contract B is A { function f() public override {} }
    """
  )

  fun testFixModifier() = checkFix(
    "Add virtual",
    """
    contract A { modifier <caret>m() { _; } }
    contract B is A { modifier m() override { _; } }
    """,
    """
    contract A { modifier m() virtual { _; } }
    contract B is A { modifier m() override { _; } }
    """
  )

  fun testBaseInOtherFile() {
    myFixture.addFileToProject("A.sol", "contract A { function f() public {} }")
    myFixture.configureByText(
      "B.sol",
      """
      import "./A.sol";
      contract B is A { function <caret>f() public override {} }
      """.trimIndent()
    )
    enableInspection()
    applyQuickFix("Make 'A.f' virtual")
    val a = com.intellij.psi.PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir("A.sol"))!!
    assertEquals("contract A { function f() public virtual {} }", a.text)
  }

  // with an external formatter (Foundry) or none, IntelliJ's formatter is disabled and can't clean up whitespace
  fun testFixWithoutIdeFormatter() = withFormatter(FormatterType.DISABLED) {
    checkFix(
      "Make 'A.f' virtual",
      """
      contract A { function f() public view returns (uint) { return 1; } }
      contract B is A { function <caret>f() public view override returns (uint) { return 2; } }
      """,
      """
      contract A { function f() public view virtual returns (uint) { return 1; } }
      contract B is A { function f() public view override returns (uint) { return 2; } }
      """
    )
  }

  fun testFixBeforeOverrideWithoutIdeFormatter() = withFormatter(FormatterType.DISABLED) {
    checkFix(
      "Add virtual",
      """
      contract A { function f() public virtual {} }
      contract B is A { function <caret>f() public override {} }
      contract C is B { function f() public override {} }
      """,
      """
      contract A { function f() public virtual {} }
      contract B is A { function f() public virtual override {} }
      contract C is B { function f() public override {} }
      """
    )
  }

  private fun withFormatter(type: FormatterType, block: () -> Unit) {
    val settings = SoliditySettings.getInstance(project)
    val old = settings.formatterType
    settings.formatterType = type
    try { block() } finally { settings.formatterType = old }
  }

  private fun checkFix(fixName: String, @Language("Solidity") before: String, @Language("Solidity") after: String) {
    myFixture.configureByText("main.sol", before.trimIndent())
    enableInspection()
    applyQuickFix(fixName)
    myFixture.checkResult(after.trimIndent())
  }
}
