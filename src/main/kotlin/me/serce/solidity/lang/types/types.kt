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

object SolString : SolPrimitiveType {
  override fun toString() = "string"
}

object SolAddress : SolPrimitiveType {
  override fun toString() = "address"
}

data class SolInteger(val unsigned: Boolean, val size: Int) : SolPrimitiveType {
  companion object {
    val INT = SolInteger(false, 256)

    fun parse(name: String): SolInteger {
      var unsigned = false
      var size = 256
      var typeName = name
      if (name.startsWith("u")) {
        unsigned = true
        typeName = typeName.substring(1)
      }
      if (!typeName.startsWith("int")) {
        throw IllegalArgumentException("Incorrect int typename: $name")
      }
      typeName = typeName.substring(3)
      if (typeName.isNotEmpty()) {
        try {
          size = Integer.parseInt(typeName)
        } catch (e: NumberFormatException) {
          throw IllegalArgumentException("Incorrect int typename: $name")
        }
      }
      return SolInteger(unsigned, size)
    }
  }

  override fun toString(): String {
    return "${if (unsigned) "u" else ""}int$size"
  }
}














