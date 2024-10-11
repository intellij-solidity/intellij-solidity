package me.serce.solidity.ide.navigation

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.utils.SolTestBase

class ContractInCommentReferenceTest : SolTestBase() {

  fun testFindImplementations() {
    val fileName = "ctr.sol"
    InlineFile(
      """
        contract A {
            function foo() public virtual {}
        }
        contract B is A { 
            /**
            * @inheritdoc A/*caret*/
            */
            function foo() public override {
            }
      }
    """, name = fileName
    ).withCaret()
    val reference = myFixture.getReferenceAtCaretPositionWithAssertion(fileName)
    val resolved = reference.resolve()
    val contract = resolved as? SolContractDefinition ?: error("Reference resolved as '${resolved?.text ?: "<null>"}' is not a contract")
    assertEquals(contract.name, "A")
  }

}
