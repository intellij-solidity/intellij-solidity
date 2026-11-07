package me.serce.solidity.lang.resolve

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.SolNamedElement
import me.serce.solidity.lang.resolve.function.SolOverrideResolver
import me.serce.solidity.utils.SolTestBase
import org.intellij.lang.annotations.Language

class SolOverrideResolverTest : SolTestBase() {

  private fun contracts(@Language("Solidity") code: String): Map<String, SolContractDefinition> =
    InlineFile(code).psiFile.children.filterIsInstance<SolContractDefinition>().associateBy { it.name!! }

  private fun SolContractDefinition.member(name: String): SolNamedElement =
    SolOverrideResolver.overridableMembers(this).first {
      (it.name ?: (it as SolFunctionDefinition).specialFunction?.name?.lowercase()) == name
    }

  private fun overridden(c: SolContractDefinition, name: String) =
    SolOverrideResolver.directlyOverridden(c.member(name)).map { SolOverrideResolver.label(it) }

  private fun bases(c: SolContractDefinition, name: String) =
    SolOverrideResolver.visibleDeclaringBases(c, c.member(name)).map { it.name }

  fun testSimpleOverride() {
    val c = contracts("""
      contract A { function f() public virtual {} }
      contract B is A { function f() public override {} }
    """)
    assertEquals(listOf("A.f"), overridden(c.getValue("B"), "f"))
    assertEquals(listOf("A"), bases(c.getValue("B"), "f"))
  }

  fun testDirectBaseVisibleEvenIfOverridden() {
    val c = contracts("""
      contract A { function f() public virtual {} }
      contract B is A { function f() public virtual override {} }
      contract C is A, B { function f() public override(A, B) {} }
    """)
    // A is a direct base, so it stays visible even though B overrides it (solc requires override(A, B))
    assertEquals(listOf("B", "A"), bases(c.getValue("C"), "f"))
  }

  fun testDiamondOneSideOverrides() {
    val c = contracts("""
      contract A { function f() public virtual {} }
      contract B is A { function f() public virtual override {} }
      contract C is A {}
      contract D is B, C { function f() public override(A, B) {} }
    """)
    assertEquals(listOf("B", "A"), bases(c.getValue("D"), "f"))
  }

  fun testIndirectGrandparentShadowed() {
    val c = contracts("""
      contract A { function f() public virtual {} }
      contract B is A { function f() public virtual override {} }
      contract C is B { function f() public override {} }
    """)
    assertEquals(listOf("B"), bases(c.getValue("C"), "f"))
  }

  fun testTwoUnrelatedBases() {
    val c = contracts("""
      contract A { function f() public virtual {} }
      contract B { function f() public virtual {} }
      contract C is A, B { function f() public override(A, B) {} }
    """)
    assertEquals(listOf("B", "A"), bases(c.getValue("C"), "f"))
  }

  fun testOverloadIsNotOverride() {
    val c = contracts("""
      contract A { function f(uint x) public virtual {} }
      contract B is A { function f(address x) public {} }
    """)
    assertEmpty(overridden(c.getValue("B"), "f"))
  }

  fun testUnresolvedTypesNeverMatch() {
    val c = contracts("""
      contract A { function f(Foo x) public virtual {} }
      contract B is A { function f(Foo x) public {} }
    """)
    assertEmpty(overridden(c.getValue("B"), "f"))
  }

  fun testModifierByName() {
    val c = contracts("""
      contract A { modifier m(uint x) virtual { _; } }
      contract B is A { modifier m(uint x) override { _; } }
    """)
    assertEquals(listOf("A.m"), overridden(c.getValue("B"), "m"))
  }

  fun testReceiveFallback() {
    val c = contracts("""
      contract A { receive() external payable virtual {} fallback() external virtual {} }
      contract B is A { receive() external payable override {} }
    """)
    assertEquals(listOf("A.receive"), overridden(c.getValue("B"), "receive"))
  }

  fun testPrivateIgnored() {
    val c = contracts("""
      contract A { function f() private {} }
      contract B is A { function f() private {} }
    """)
    assertEmpty(overridden(c.getValue("B"), "f"))
  }

  fun testIsVirtual() {
    val c = contracts("""
      interface I { function f() external; }
      contract A { function g() public {} function h() public virtual {} }
    """)
    assertTrue(SolOverrideResolver.isVirtual(c.getValue("I").member("f")))
    assertFalse(SolOverrideResolver.isVirtual(c.getValue("A").member("g")))
    assertTrue(SolOverrideResolver.isVirtual(c.getValue("A").member("h")))
  }

  fun testOverridesInDerived() {
    val c = contracts("""
      contract A { function f() public {} }
      contract B is A { function f() public {} }
    """)
    assertEquals(listOf("B.f"), SolOverrideResolver.overridesInDerived(c.getValue("A").member("f")).map { SolOverrideResolver.label(it) })
  }
}
