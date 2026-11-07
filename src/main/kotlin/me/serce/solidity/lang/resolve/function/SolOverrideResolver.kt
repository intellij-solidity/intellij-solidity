package me.serce.solidity.lang.resolve.function

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

  /**
   * Bases of [contract] that declare a member with the same signature as [member] and are reachable along at
   * least one inheritance path without another declaring contract in between. Ordered most derived first.
   */
  fun visibleDeclaringBases(contract: SolContractDefinition, member: SolNamedElement): List<SolContractDefinition> {
    val order = linearizedParents(contract) ?: return emptyList()
    val result = LinkedHashSet<SolContractDefinition>()
    val visited = HashSet<SolContractDefinition>()
    fun walk(c: SolContractDefinition) {
      for (p in SolContract(c).getParents().map { it.ref }) {
        if (overridableMembers(p).any { participates(it) && sameSignature(it, member) }) {
          result += p
        } else if (visited.add(p)) {
          walk(p)
        }
      }
    }
    walk(contract)
    return result.sortedBy { order.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }
  }

  /** Base declarations that [member] directly overrides. */
  fun directlyOverridden(member: SolNamedElement): List<SolNamedElement> {
    if (!participates(member)) return emptyList()
    val contract = containingContract(member) ?: return emptyList()
    return visibleDeclaringBases(contract, member).mapNotNull { base ->
      overridableMembers(base).firstOrNull { participates(it) && sameSignature(it, member) }
    }
  }

  /** Members of derived contracts that override [decl] (directly or indirectly). Uses the inheritance index. */
  fun overridesInDerived(decl: SolNamedElement): List<SolNamedElement> {
    if (!participates(decl)) return emptyList()
    val contract = containingContract(decl) ?: return emptyList()
    return contract.findAllImplementations()
      .flatMap { overridableMembers(it) }
      .filter { participates(it) && sameSignature(it, decl) }
  }

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

  /** "Contract.member" label used in messages. */
  fun label(member: SolNamedElement): String {
    val name = (member as? SolFunctionDefinition)?.specialFunction?.name?.lowercase() ?: member.name
    return "${containingContract(member)?.name}.$name"
  }

  private fun linearizedParents(contract: SolContractDefinition): List<SolContractDefinition>? = try {
    SolContract(contract).linearizeParents().map { it.ref }
  } catch (e: LinearizationImpossibleException) {
    null
  }
}
