package me.serce.solidity.lang.types

// http://solidity.readthedocs.io/en/develop/types.html

interface SolType
interface SolPrimitiveType : SolType

object SolUnknown : SolPrimitiveType {
  override fun toString() = "<unknown>"
}

object SolBoolean : SolPrimitiveType {
  override fun toString() = "bool"
}

object SolInteger : SolPrimitiveType {

}














