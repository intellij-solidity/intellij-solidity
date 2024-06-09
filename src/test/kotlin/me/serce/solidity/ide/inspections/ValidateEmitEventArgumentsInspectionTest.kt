import me.serce.solidity.ide.inspections.SolInspectionsTestBase
import me.serce.solidity.ide.inspections.ValidateEmitEventArgumentsInspection

class ValidateEmitEventArgumentsInspectionTest : SolInspectionsTestBase(ValidateEmitEventArgumentsInspection()) {
  fun testNoArguments() = checkByText("""
        contract a {
            event TestEvent(uint a);
            function a() returns (uint result) {
            /*@error descr="No arguments"@*/emit TestEvent()/*@/error@*/;
                return 1;
            }
        }
    """)

  fun testArgumentsLengthNotMatch() = checkByText("""
        contract a {
            event TestEvent(uint a, uint b);
            function a() returns (uint result) {
            /*@error descr="Expected 2 arguments, but got 1"@*/emit TestEvent(1)/*@/error@*/;
                return 1;
            }
        }
    """)

  fun testArgumentsTypeNotMatch() = checkByText("""
        contract a {
            event TestEvent(uint a, uint b);
            function a() returns (uint result) {
            /*@error descr="Argument of type 'string' is not assignable to parameter of type 'uint256'"@*/emit TestEvent(1, "abc")/*@/error@*/;
                return 1;
            }
        }
    """)

  fun testNoErrors() = checkByText("""
        contract a {
            event TestEvent(uint a, uint b);
            function a() returns (uint result) {
                emit TestEvent(1, 2);
                return 1;
            }
        }
    """)
}
