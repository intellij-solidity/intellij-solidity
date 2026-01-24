package me.serce.solidity.lang.resolve.function

import me.serce.solidity.ide.navigation.findAllImplementations
import me.serce.solidity.ide.navigation.findImplementations
import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolFunctionDefElement
import me.serce.solidity.lang.psi.SolFunctionDefinition
import me.serce.solidity.lang.psi.impl.LinearizationImpossibleException
import me.serce.solidity.lang.psi.parentOfType
import me.serce.solidity.lang.types.SolContract
import me.serce.solidity.lang.types.getSolType

object SolFunctionResolver {
  fun collectOverridden(func: SolFunctionDefElement, contract: SolContractDefinition? = func.parentOfType()): Collection<SolFunctionDefinition> {
    contract ?: return emptyList()
    val parents = try {
      SolContract(contract).linearizeParents().map { it.ref }
    } catch (e: LinearizationImpossibleException) {
      emptyList<SolContractDefinition>()
    }
    return parents
      .flatMap { it.functionDefinitionList }
      .filter { signatureEquals(func, it) }
  }

  fun collectOverrides(func: SolFunctionDefinition): Collection<SolFunctionDefinition> {
    val contract = func.parentOfType<SolContractDefinition>() ?: return emptyList()
    return contract.findAllImplementations()
      .flatMap { it.functionDefinitionList }
      .filter { signatureEquals(func, it) }
  }

  fun hasOverrides(func: SolFunctionDefinition): Boolean {
    val contract = func.parentOfType<SolContractDefinition>() ?: return false
    return contract.findImplementations().flatMap { it.functionDefinitionList }.any { signatureEquals(func, it) }
  }

  private fun signatureEquals(f1: SolFunctionDefElement, f2: SolFunctionDefElement): Boolean {
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
    return true
  }
}
