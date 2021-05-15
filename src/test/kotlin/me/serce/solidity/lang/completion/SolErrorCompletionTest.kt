package me.serce.solidity.lang.completion

class SolErrorCompletionTest : SolCompletionTestBase() {

  fun testErrorCompletion() = checkCompletion(hashSetOf("BaseError"), """
        contract Base {
            error BaseError();

            function emitError() {
                revert /*caret*/
            }
        }
  """)

  fun testErrorWithInheritance() = checkCompletion(hashSetOf("BaseError", "ChildError"), """
        contract BaseContract {
            error BaseError();
        }

        contract ChildContract is BaseContract {

            error ChildError();

            function emitError() {
                revert /*caret*/
            }
        }
  """)

  fun testErrorExactCompletion() = checkCompletion(hashSetOf("ChildError", "CopyError"), """
        contract BaseContract {
            error BaseError();
        }

        contract ChildContract is BaseContract {

            error ChildError();
            error CopyError();

            function emitError() {
                revert C/*caret*/
            }
        }
  """)
}
