package me.serce.solidity.lang.resolve.function

import com.intellij.psi.PsiElement
import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.LinearizationImpossibleException
import me.serce.solidity.lang.types.SolContract

/**
 * Override relations between functions/modifiers of contracts, following Solidity 0.8.x rules.
 *
 * Members are [SolFunctionDefinition] (excluding constructors) and [SolModifierDefinition].
 */
object SolOverrideResolver {

  fun overridableMembers(contract: SolContractDefinition): List<SolNamedElement> =
    contract.functionDefinitionList.filter { !it.isConstructor } + contract.modifierDefinitionList

  fun containingContract(member: SolNamedElement): SolContractDefinition? = when (member) {
    is SolFunctionDefinition -> member.contract
    is SolModifierDefinition -> member.parent as? SolContractDefinition
    else -> null
  }

  fun sameSignature(a: SolNamedElement, b: SolNamedElement): Boolean = when {
    a is SolFunctionDefinition && b is SolFunctionDefinition -> {
      if (a.specialFunction != null || b.specialFunction != null) {
        a.specialFunction == b.specialFunction
      } else {
        SolFunctionResolver.signatureEquals(a, b)
      }
    }
    a is SolModifierDefinition && b is SolModifierDefinition -> a.name != null && a.name == b.name
    else -> false
  }

  /** Whether [member] can take part in overriding at all (as overrider or overridden). */
  fun participates(member: SolNamedElement): Boolean {
    val contract = containingContract(member) ?: return false
    if (contract.contractType == ContractType.LIBRARY) return false
    if (member is SolFunctionDefinition) {
      if (member.isConstructor) return false
      if (member.visibility == Visibility.PRIVATE) return false
      if (member.specialFunction == null && member.name == null) return false
    }
    return true
  }

  /** The base contracts a member overrides (see [visibleDeclaringBases]) and its declaration in each of them. */
  data class Overrides(val bases: List<SolContractDefinition>, val declarations: List<SolNamedElement>) {
    fun isEmpty() = declarations.isEmpty()
  }

  /**
   * Bases of [contract] that declare a member with the same signature as [member] and are reachable along at
   * least one inheritance path without another declaring contract in between. Ordered most derived first.
   */
  fun visibleDeclaringBases(contract: SolContractDefinition, member: SolNamedElement): List<SolContractDefinition> {
    val order = linearizedParents(contract) ?: return emptyList()
    val found = HashSet<SolContractDefinition>()
    val visited = HashSet<SolContractDefinition>()
    fun walk(c: SolContractDefinition) {
      for (p in SolContract(c).getParents().map { it.ref }) {
        if (!visited.add(p)) continue
        if (declarationIn(p, member) != null) found += p else walk(p)
      }
    }
    walk(contract)
    // the linearization is already ordered most derived first
    return order.filter { it in found }
  }

  /** What [member] directly overrides; empty when it doesn't take part in overriding. */
  fun overrides(member: SolNamedElement): Overrides {
    val contract = containingContract(member)
    if (contract == null || !participates(member)) return Overrides(emptyList(), emptyList())
    val bases = visibleDeclaringBases(contract, member)
    return Overrides(bases, bases.mapNotNull { declarationIn(it, member) })
  }

  /** Base declarations that [member] directly overrides. */
  fun directlyOverridden(member: SolNamedElement): List<SolNamedElement> = overrides(member).declarations

  /** Whether a member of a derived contract overrides [decl] (directly or indirectly). Uses the inheritance index. */
  fun isOverriddenInDerived(decl: SolNamedElement): Boolean {
    if (!participates(decl)) return false
    val contract = containingContract(decl) ?: return false
    return contract.findAllImplementations().any { declarationIn(it, decl) != null }
  }

  private fun declarationIn(contract: SolContractDefinition, member: SolNamedElement): SolNamedElement? =
    overridableMembers(contract).firstOrNull { participates(it) && sameSignature(it, member) }

  fun isVirtual(decl: SolNamedElement): Boolean =
    virtualSpecifier(decl) != null || containingContract(decl)?.contractType == ContractType.INTERFACE

  fun hasBody(decl: SolNamedElement): Boolean = when (decl) {
    is SolFunctionDefinition -> decl.block != null
    else -> true
  }

  fun overrideSpecifier(member: SolNamedElement): SolOverrideSpecifier? = when (member) {
    is SolFunctionDefinition -> member.functionVisibilitySpecifierList.firstNotNullOfOrNull { it.overrideSpecifier }
    is SolModifierDefinition -> member.overrideSpecifierList.firstOrNull()
    else -> null
  }

  fun virtualSpecifier(member: SolNamedElement): SolVirtualSpecifier? = when (member) {
    is SolFunctionDefinition -> member.functionVisibilitySpecifierList.firstNotNullOfOrNull { it.virtualSpecifier }
    is SolModifierDefinition -> member.virtualSpecifierList.firstOrNull()
    else -> null
  }

  /** Member name; `receive`/`fallback` for the special functions. */
  fun displayName(member: SolNamedElement): String? =
    (member as? SolFunctionDefinition)?.specialFunction?.name?.lowercase() ?: member.name

  /** Source text of the member's parameter list, e.g. `(uint256 x)`; null for a modifier without one. */
  fun parametersText(member: SolNamedElement): String? = when (member) {
    is SolFunctionDefinition -> member.parameterListList.firstOrNull()?.text
    is SolModifierDefinition -> member.parameterList?.text
    else -> null
  }

  /** Name identifier, or the `receive`/`fallback` keyword; where problems about the member are reported. */
  fun nameElement(member: SolNamedElement): PsiElement? = when (member) {
    is SolFunctionDefinition -> member.identifier ?: member.firstChild
    is SolModifierDefinition -> member.identifier
    else -> null
  }

  /** "Contract.member" label used in messages. */
  fun label(member: SolNamedElement): String = "${containingContract(member)?.name}.${displayName(member)}"

  /** Linearized parents, most derived first; null when linearization is impossible. */
  fun linearizedParents(contract: SolContractDefinition): List<SolContractDefinition>? = try {
    SolContract(contract).linearizeParents().map { it.ref }
  } catch (e: LinearizationImpossibleException) {
    null
  }
}
