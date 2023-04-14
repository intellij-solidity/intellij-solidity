package me.serce.solidity.ide.inspections

class ValidateFunctionArgumentsInspectionTest : SolInspectionsTestBase(ValidateFunctionArgumentsInspection()) {
  fun testTypeAssertFailure() = checkByText("""
        contract a {
            function a() {
                assert(/*@error descr="Argument of type 'string' is not assignable to parameter of type 'bool'"@*/"myString"/*@/error@*/);
            }
        }
    """)

  fun testTypeAssertSuccess() = checkByText("""
        contract a {
            function a() {
                assert(true);
            }
        }
    """)

  fun testWrongNumberOfArguments() = checkByText("""
        contract a {
            function a() {
                b(/*@error descr="Expected 1 argument, but got 2"@*/1, 2/*@/error@*/);
            }
            
            function b(int a) {
            
            }
        }
    """)

  fun testValidationDisabled() = checkByText("""
        contract a {
            function a() {
                b(1, 2);
            }
            
            /**
            * @custom:no_validation
            */
            function b(int a) {
            
            }
        }
    """)

}
