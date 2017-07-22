package me.serce.solidity.ide.inspections

class SelfdestructInspectionTest : SolInspectionsTestBase(SelfdestructRenameInspection()) {
  fun test() = checkByText("""
        contract a {
            function a() {
                /*@warning descr="suicide is deprecated. rename to selfdestruct. EIP 6"@*/suicide(owner)/*@/warning@*/;
            }
        }
    """)
}
