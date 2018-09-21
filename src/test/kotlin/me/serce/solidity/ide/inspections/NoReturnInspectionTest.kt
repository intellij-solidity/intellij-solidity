package me.serce.solidity.ide.inspections

class NoReturnInspectionTest : SolInspectionsTestBase(NoReturnInspection()) {
  fun test() = checkByText("""
        contract a {
            /*@warning descr="no return statement"@*/function a() returns (bool) {
                var test = 5;
            }/*@/warning@*/
        }
    """)
}
