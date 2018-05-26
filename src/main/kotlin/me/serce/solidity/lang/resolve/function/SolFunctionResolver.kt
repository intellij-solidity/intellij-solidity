package me.serce.solidity.lang.resolve.function

import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.types.getSolType

object SolFunctionResolver {
  fun collectOverriden(func: SolFunctionDefinition): Collection<SolFunctionDefinition> {
    val contract = func.parentOfType<SolContractDefinition>() ?: return emptyList()
    return contract.collectSupers
      .mapNotNull { it.reference?.resolve() }
      .filterIsInstance<SolContractDefinition>()
      .flatMap { it.functionDefinitionList }
      .filter { signatureEquals(func, it) }
  }

  fun collectOverrides(func: SolFunctionDefinition): Collection<SolFunctionDefinition> {
    val contract = func.parentOfType<SolContractDefinition>() ?: return emptyList()
    return contract.findAllImplementations()
      .flatMap { it.functionDefinitionList }
      .filter { signatureEquals(func, it) }
  }

  private fun signatureEquals(f1: SolFunctionDefinition, f2: SolFunctionDefinition): Boolean {
    if (f1.name != f2.name) {
      return false
    }
    if (f1.parameters.size != f2.parameters.size) {
      return false
    }
    for ((p1, p2) in f1.parameters.zip(f2.parameters)) {
      if (getSolType(p1.typeName) != getSolType(p2.typeName)) {
        return false
      }
    }
    return true;
  }
}
