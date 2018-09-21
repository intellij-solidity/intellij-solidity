package me.serce.solidity.ide.inspections

class NoReturnInspectionTest : SolInspectionsTestBase(NoReturnInspection()) {
  fun testNoReturnVarName() = checkByText("""
        contract a {
            /*@warning descr="no return statement"@*/function a() returns (uint) {
                var test = 5;
            }/*@/warning@*/
        }
    """)

  fun testReturnWithIf() = checkByText("""
        contract a {
            function a() returns (uint) {
                if (true) {
                  return 1;
                } else {
                  return 2;
                }
            }
        }
    """)

  fun testAssignmentWithIf() = checkByText("""
        contract a {
            function a() returns (uint result) {
                if (true) {
                  result = 1;
                } else {
                  result = 2;
                }
            }
        }
    """)

  fun testHasReturnVarName() = checkByText("""
        contract a {
            function a() returns (/*@warning descr="return variable not assigned"@*/uint a/*@/warning@*/) {
                var test = 5;
            }
        }
    """)

  fun testHasReturnVarNameWithVarDef() = checkByText("""
        contract a {
            function a() returns (uint a) {
                var a = 5;
            }
        }
    """)
}
