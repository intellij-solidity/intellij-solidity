package me.serce.solidity.ide.inspections

class NoReturnInspectionTest : SolInspectionsTestBase(NoReturnInspection()) {
  fun testNoReturnVarName() = checkByText("""
        contract a {
            /*@warning descr="no return statement"@*/function a() returns (uint) {
                var test = 5;
            }/*@/warning@*/
        }
    """)

  fun testRevert() = checkByText("""
        contract a {
            function a() returns (uint) {
                revert();
            }
        }
    """)

  fun testRevertShadowing() = checkByText("""
        contract a {
            function revert() {

            }

            /*@warning descr="no return statement"@*/function a() returns (uint) {
                revert();
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

  fun testThrow() = checkByText("""
        contract a {
            function payReward() returns(bool) {
                if (usnContract.getOriginalClient().DAOrewardAccount().call.value(msg.value)()) {
                    return true;
                } else {
                    throw;
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

  fun testReturnVarInTuple() = checkByText("""
        contract a {
            function a() returns (uint a) {
                var test = 5;
                (a, test) = (5, 5);
            }
        }
    """)

  fun testAssignmentInAssembly() = checkByText("""
        contract a {
            function lowLevel(string memory source) constant returns (bytes32 result) {
                assembly {
                    result := mload(add(source, 32))
                }
            }
        }
    """)

  fun testAssignmentWithFunctionCall() = checkByText("""
        interface SomeInterface {

        }

        contract a {
            address someAddress;

            function lookupService(bytes32 identifier) constant returns (address manager) {
                manager = SomeInterface(someAddress).getContractAddress(identifier);
            }
        }
    """)
}
