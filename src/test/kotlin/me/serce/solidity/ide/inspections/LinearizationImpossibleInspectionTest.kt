package me.serce.solidity.ide.inspections

class LinearizationImpossibleInspectionTest : SolInspectionsTestBase(LinearizationImpossibleInspection()) {
  fun testImpossible() = checkByText("""
        contract A {}
        contract B {}
        contract C is B, A {}
        contract D is A, B {}
        contract /*@warning descr="Linearization of inheritance graph impossible: result: [C, D] lists: [[A, B], [B, A]]"@*/E/*@/warning@*/ is D, C {}
    """)

  fun testPossible() = checkByText("""
        contract Ownable {} // Ownable
        contract WhitelistAdminRole is Ownable {} // WhitelistAdminRole Ownable
        contract ReferrerProviderImpl is Ownable, WhitelistAdminRole {}
    """)

}
