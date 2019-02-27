package me.serce.solidity.ide.inspections

class LinearizationImpossibleTest : SolInspectionsTestBase(LinearizationImpossibleInspection()) {
  fun testImpossible() = checkByText("""
        contract A {}
        contract B {}
        contract C is A, B {}
        contract D is B, A {}
        contract /*@warning descr="Linearization of inheritance graph impossible: result: [C, D] lists: [[A, B], [B, A]]"@*/E/*@/warning@*/ is C, D {}
    """)

}
