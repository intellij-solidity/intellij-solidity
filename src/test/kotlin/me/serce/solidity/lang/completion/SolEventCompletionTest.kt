package me.serce.solidity.lang.completion

class SolEventCompletionTest : SolCompletionTestBase() {

  fun testEventCompletion() = checkCompletion(hashSetOf("BaseEvent"), """
        contract Base {
            event BaseEvent();

            function emitEvent() {
                emit /*caret*/
            }
        }
  """)

  fun testEventWithInheritance() = checkCompletion(hashSetOf("BaseEvent", "ChildEvent"), """
        contract BaseContract {
            event BaseEvent();
        }

        contract ChildContract is BaseContract {

            event ChildEvent();

            function emitEvent() {
                emit /*caret*/
            }
        }
  """)

  fun testEventExactCompletion() = checkCompletion(hashSetOf("ChildEvent", "CopyEvent"), """
        contract BaseContract {
            event BaseEvent();
        }

        contract ChildContract is BaseContract {

            event ChildEvent();
            event CopyEvent();

            function emitEvent() {
                emit C/*caret*/
            }
        }
  """)
}
