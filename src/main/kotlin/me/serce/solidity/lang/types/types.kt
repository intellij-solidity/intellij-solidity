package me.serce.solidity.lang.types

import me.serce.solidity.lang.psi.SolContractDefinition
import me.serce.solidity.lang.psi.SolEnumDefinition
import me.serce.solidity.lang.psi.SolNumberLiteral
import me.serce.solidity.lang.psi.SolStructDefinition
import me.serce.solidity.lang.psi.impl.Linearizable
import me.serce.solidity.lang.types.SolInteger.Companion.UINT_160
import java.math.BigInteger

// http://solidity.readthedocs.io/en/develop/types.html

interface SolType {
  fun isAssignableFrom(other: SolType): Boolean
}
interface SolPrimitiveType : SolType
interface SolNumeric : SolPrimitiveType

object SolUnknown : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean = false

  override fun toString() = "<unknown>"
}

object SolBoolean : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other == SolBoolean

  override fun toString() = "bool"
}

object SolString : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other == SolString

  override fun toString() = "string"
}

object SolAddress : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolAddress -> true
      is SolContract -> true
      else -> UINT_160.isAssignableFrom(other)
    }

  override fun toString() = "address"
}

data class SolInteger(val unsigned: Boolean, val size: Int) : SolNumeric {
  companion object {
    val UINT_160 = SolInteger(true, 160)
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

    fun inferType(numberLiteral: SolNumberLiteral): SolInteger {
      return inferIntegerType(numberLiteral.toBigInteger())
    }

    fun inferIntegerType(value: BigInteger): SolInteger {
      if (value == BigInteger.ZERO) return SolInteger(true, 8)
      val positive = value >= BigInteger.ZERO
      if (positive) {
        var shifts = 0
        var current = value
        while (current != BigInteger.ZERO) {
          shifts++
          current = current.shiftRight(8)
        }
        return SolInteger(positive, shifts * 8)
      } else {
        var shifts = 1
        var current = value.abs().minus(BigInteger.ONE).shiftRight(7)
        while (current != BigInteger.ZERO) {
          shifts++
          current = current.shiftRight(8)
        }
        return SolInteger(positive, shifts * 8)
      }
    }

    private fun SolNumberLiteral.toBigInteger(): BigInteger {
      this.decimalNumber?.let {
        return it.text.toBigInteger()
      }
      this.hexNumber?.let {
        return it.text.removePrefix("0x").toBigInteger(16)
      }
      this.scientificNumber?.let {
        val eIndex = it.text.toLowerCase().indexOf('e')

      }
      //todo
      return BigInteger.ZERO
    }
  }

  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolInteger -> {
        if (this.unsigned && !other.unsigned) {
          false
        } else if (!this.unsigned && other.unsigned) {
          this.size * 2 >= other.size
        } else {
          this.size >= other.size
        }
      }
      else -> false
    }

  override fun toString() = "${if (unsigned) "u" else ""}int$size"
}

data class SolContract(val ref: SolContractDefinition) : SolType, Linearizable<SolContract> {
  override fun getParents(): List<SolContract> {
    return ref.supers
      .flatMap { it.reference?.multiResolve() ?: emptyList() }
      .filterIsInstance<SolContractDefinition>()
      .map { SolContract(it) }
  }

  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolContract -> {
        other.ref == ref
          || other.ref.collectSupers.filterIsInstance<SolContractDefinition>().contains(ref)
      }
      else -> false
    }

  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolStruct(val ref: SolStructDefinition) : SolType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolStruct && ref == other.ref

  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolEnum(val ref: SolEnumDefinition) : SolType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolEnum && ref == other.ref

  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolMapping(val from: SolType, val to: SolType) : SolType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolMapping && from == other.from && to == other.to

  override fun toString(): String {
    return "mapping($from => $to)"
  }
}

data class SolTuple(val types: List<SolType>) : SolType {
  override fun isAssignableFrom(other: SolType): Boolean = false

  override fun toString(): String {
    return "(${types.joinToString(separator = ",") { it.toString() }})"
  }
}

sealed class SolArray(val type: SolType) : SolType {
  class SolStaticArray(type: SolType, val size: Int) : SolArray(type) {
    override fun isAssignableFrom(other: SolType): Boolean =
      other is SolStaticArray && other.type == type && other.size == size

    override fun toString() = "$type[$size]"
  }

  class SolDynamicArray(type: SolType) : SolArray(type) {
    override fun isAssignableFrom(other: SolType): Boolean =
      other is SolDynamicArray && type == other.type

    override fun toString() = "$type[]"
  }
}

object SolBytes : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other == SolBytes

  override fun toString() = "bytes"
}

data class SolFixedBytes(val size: Int): SolPrimitiveType {
  override fun toString() = "bytes$size"

  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolFixedBytes && other.size <= size

  companion object {
    fun parse(name: String): SolFixedBytes {
      return if (name.startsWith("bytes")) {
        SolFixedBytes(name.substring(5).toInt())
      } else {
        throw java.lang.IllegalArgumentException("should start with bytes")
      }
    }
  }
}

private val INTENAL_INDICATOR = "_sol1_s"

fun internalise(name: String): String = "$name$INTENAL_INDICATOR"

fun isInternal(name: String): Boolean = name.endsWith(INTENAL_INDICATOR)

fun deInternalise(name: String): String = when {
  name.endsWith(INTENAL_INDICATOR) -> name.removeSuffix(INTENAL_INDICATOR)
  else -> name
}
