package me.serce.solidity.ide.inspections

class VisibilityModifierInspectionTest : SolInspectionsTestBase(VisibilityModifierInspection()) {
  fun testTypeAssertFailure() = checkByText("""
        contract a {
            uint /*@weak_warning descr="No visibility modifier"@*/b/*@/weak_warning@*/;
            function /*@weak_warning descr="No visibility modifier"@*/a/*@/weak_warning@*/() {
                int i = 1;
            }
        }
    """, checkWeakWarn = true)

  fun testTypeAssertSuccess() = checkByText("""
        contract a {
            uint private b;
            function a() public {
                
            }
        }
    """)


}
