package me.serce.solidity.lang.completion

import com.intellij.openapi.application.ApplicationManager
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.childOfType
import java.util.concurrent.TimeUnit

class ContractLookupElementTest : SolCompletionTestBase() {
  fun testContractLookupStringAvailableOutsideReadAction() {
    val file = InlineFile("contract Foo {}").psiFile
    val contract = file.childOfType<SolContractDefinition>()!!
    val lookup = ContractLookupElement(contract)

    val future = ApplicationManager.getApplication().executeOnPooledThread<String> {
      lookup.lookupString
    }
    assertEquals("Foo", future.get(5, TimeUnit.SECONDS))
  }
}
