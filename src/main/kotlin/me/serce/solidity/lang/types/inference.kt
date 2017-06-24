package me.serce.solidity.lang.types

import me.serce.solidity.firstOrElse
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.resolve.SolResolver

private fun getSolType(solTypeName: SolTypeName?): SolType {
  return when(solTypeName) {
    is SolElementaryTypeName -> when(solTypeName.firstChild.text) {
      "bool" -> SolBoolean
      else -> SolUnknown
    }
    else -> SolUnknown
  }
}

fun inferDeclType(decl: SolNamedElement): SolType {
  return when(decl) {
    is SolVariableDeclaration -> getSolType(decl.typeName)
    else -> SolUnknown
  }
}

fun inferRefType(ref: SolReferenceElement): SolType {
  return when (ref) {
    is SolVarLiteral -> {
      val declarations = SolResolver.resolveVarLiteral(ref)
      return declarations.asSequence()
        .map { inferDeclType(it) }
        .filter { it != SolUnknown }
        .firstOrElse(SolUnknown)
    }
    else -> SolUnknown
  }
}
