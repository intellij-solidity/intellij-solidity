package me.serce.solidity.ide.inspections

import org.intellij.lang.annotations.Language

class MissingOverrideInspectionTest : SolInspectionsTestBase(MissingOverrideInspection()) {

  fun testRemixExample() = checkByText("""
    contract SimpleStorage {
        uint256 myFavoriteNumber;
        function store(uint256 _favoriteNumber) public virtual {
            myFavoriteNumber = _favoriteNumber;
        }
    }
    contract AddFiveStorage is SimpleStorage {
        function /*@error descr="Function overrides 'SimpleStorage.store' but is missing 'override'"@*/store/*@/error@*/(uint256 _favoriteNumber) public {}
    }
  """)

  fun testWithOverrideOk() = checkByText("""
    contract A { function f() public virtual {} }
    contract B is A { function f() public override {} }
  """)

  fun testSingleInterfaceNoOverrideOk() = checkByText("""
    interface I { function f() external; }
    contract C is I { function f() external {} }
  """)

  fun testTwoInterfacesRequireOverride() = checkByText("""
    interface I { function f() external; }
    interface J { function f() external; }
    contract C is I, J { function /*@error descr="Function overrides 'J.f' but is missing 'override'"@*/f/*@/error@*/() external {} }
  """)

  fun testBareOverrideNeedsList() = checkByText("""
    contract A { function f() public virtual {} }
    contract B { function f() public virtual {} }
    contract C is A, B { function f() public /*@error descr="Must specify overridden contracts: override(B, A)"@*/override/*@/error@*/ {} }
  """)

  fun testExplicitListOk() = checkByText("""
    contract A { function f() public virtual {} }
    contract B { function f() public virtual {} }
    contract C is A, B { function f() public override(A, B) {} }
  """)

  fun testModifier() = checkByText("""
    contract A { modifier m() virtual { _; } }
    contract B is A { modifier /*@error descr="Modifier overrides 'A.m' but is missing 'override'"@*/m/*@/error@*/() { _; } }
  """)

  fun testOverloadNoError() = checkByText("""
    contract A { function f(uint x) public virtual {} }
    contract B is A { function f(address x) public {} }
  """)

  fun testFixAddsOverride() = checkFix(
    "Add override",
    """
    contract A { function store(uint256 _x) public virtual {} }
    contract B is A { function st<caret>ore(uint256 _x) public {} }
    """,
    """
    contract A { function store(uint256 _x) public virtual {} }
    contract B is A { function store(uint256 _x) public override {} }
    """
  )

  fun testFixAddsOverrideAfterMutability() = checkFix(
    "Add override",
    """
    contract A { function get() public view virtual returns (uint) { return 1; } }
    contract B is A { function g<caret>et() public view returns (uint) { return 2; } }
    """,
    """
    contract A { function get() public view virtual returns (uint) { return 1; } }
    contract B is A { function get() public view override returns (uint) { return 2; } }
    """
  )

  fun testFixAddsList() = checkFix(
    "Add override(B, A)",
    """
    contract A { function f() public virtual {} }
    contract B is A { function f() public virtual override {} }
    contract C is A {}
    contract D is B, C { function <caret>f() public {} }
    """,
    """
    contract A { function f() public virtual {} }
    contract B is A { function f() public virtual override {} }
    contract C is A {}
    contract D is B, C { function f() public override(B, A) {} }
    """
  )

  fun testFixReplacesBareOverride() = checkFix(
    "Add override(B, A)",
    """
    contract A { function f() public virtual {} }
    contract B { function f() public virtual {} }
    contract C is A, B { function f() public over<caret>ride {} }
    """,
    """
    contract A { function f() public virtual {} }
    contract B { function f() public virtual {} }
    contract C is A, B { function f() public override(B, A) {} }
    """
  )

  fun testFixModifier() = checkFix(
    "Add override",
    """
    contract A { modifier m(uint x) virtual { _; } }
    contract B is A { modifier <caret>m(uint x) { _; } }
    """,
    """
    contract A { modifier m(uint x) virtual { _; } }
    contract B is A { modifier m(uint x) override { _; } }
    """
  )

  private fun checkFix(fixName: String, @Language("Solidity") before: String, @Language("Solidity") after: String) {
    myFixture.configureByText("main.sol", before.trimIndent())
    enableInspection()
    applyQuickFix(fixName)
    myFixture.checkResult(after.trimIndent())
  }
}
