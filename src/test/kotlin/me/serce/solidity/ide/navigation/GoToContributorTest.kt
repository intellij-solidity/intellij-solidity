package me.serce.solidity.ide.navigation

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.utils.SolTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class GoToContributorTest : SolTestBase() {
  fun testClassContributorFindsContracts() {
    InlineFile(
      """
      contract Foo {}
      contract Bar {}
      """
    )
    val contributor = SolClassNavigationContributor()
    val names = contributor.getNames(myFixture.project, true).toSet()
    assertTrue(names.containsAll(setOf("Foo", "Bar")))

    val items = contributor.getItemsByName("Foo", null, myFixture.project, false)
    assertEquals(1, items.size)
    val contract = items[0] as SolContractDefinition
    assertEquals("Foo", contract.name)
    assertEquals("Foo", contributor.getQualifiedName(contract))
    assertEquals(".", contributor.getQualifiedNameSeparator())
  }

  fun testSymbolContributorFindsFunction() {
    InlineFile(
      """
      contract Foo {
        function bar() public {}
      }
      """
    )
    val contributor = SolSymbolNavigationContributor()
    val names = contributor.getNames(myFixture.project, true).toSet()
    assertTrue(names.containsAll(setOf("Foo", "bar")))

    val items = contributor.getItemsByName("bar", null, myFixture.project, false)
    assertEquals(1, items.size)
    val func = items[0] as SolFunctionDefinition
    assertEquals("bar", func.name)
    assertEquals("bar", contributor.getQualifiedName(func))
  }

  fun testNullArgumentsReturnEmpty() {
    val contributor = SolClassNavigationContributor()
    assertEquals(0, contributor.getNames(null, false).size)
    assertEquals(0, contributor.getItemsByName(null, null, myFixture.project, false).size)
    assertEquals(0, contributor.getItemsByName("Foo", null, null, false).size)
  }
}

