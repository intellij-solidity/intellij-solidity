package me.serce.solidity.lang.types

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolStructDefinition

// http://solidity.readthedocs.io/en/develop/types.html

interface SolType
interface SolPrimitiveType : SolType
interface SolNumeric : SolPrimitiveType

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

data class SolInteger(val unsigned: Boolean, val size: Int) : SolNumeric {
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

  override fun toString() = "${if (unsigned) "u" else ""}int$size"
}

data class SolContract(val ref: SolContractDefinition) : SolType {
  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolStruct(val ref: SolStructDefinition) : SolType {
  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolMapping(val from: SolType, val to: SolType): SolType {
  override fun toString(): String {
    return "mapping($from => $to)"
  }
}

sealed class SolArray(val type: SolType) : SolType {
  class SolStaticArray(type: SolType, val size: Int) : SolArray(type) {
    override fun toString() = "$type[$size]"
  }

  class SolDynamicArray(type: SolType) : SolArray(type) {
    override fun toString() = "$type[]"
  }
}


private val INTENAL_INDICATOR = "_sol1_s"

fun internalise(name: String): String = "$name$INTENAL_INDICATOR"

fun isInternal(name: String): Boolean = name.endsWith(INTENAL_INDICATOR)

fun deInternalise(name: String): String = when {
    name.endsWith(INTENAL_INDICATOR) -> name.removeSuffix(INTENAL_INDICATOR)
    else -> name
}
