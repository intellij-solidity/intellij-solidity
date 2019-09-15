package me.serce.solidity.lang.types

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import me.serce.solidity.lang.psi.*
import me.serce.solidity.lang.psi.impl.Linearizable
import me.serce.solidity.lang.resolve.SolResolver
import me.serce.solidity.lang.types.SolInteger.Companion.UINT_160
import java.math.BigInteger
import java.util.*

// http://solidity.readthedocs.io/en/develop/types.html

interface SolType {
  fun isAssignableFrom(other: SolType): Boolean
  fun getBuiltinFunctions(project: Project): List<SolCallable> {
    return emptyList()
  }
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

  override fun getBuiltinFunctions(project: Project): List<SolCallable> {
    return SolInternalTypeFactory.of(project).addressType.ref.functionDefinitionList
  }
}

data class SolInteger(val unsigned: Boolean, val size: Int) : SolNumeric {
  companion object {
    val UINT_160 = SolInteger(true, 160)

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

    private fun inferIntegerType(value: BigInteger): SolInteger {
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
        return it.text.replace("_", "").toBigInteger()
      }
      this.hexNumber?.let {
        return it.text.removePrefix("0x").toBigInteger(16)
      }
      this.scientificNumber?.let {
        return it.text.replace("_", "").toLowerCase().toBigDecimal().toBigInteger()
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
          this.size - 2 >= other.size
        } else {
          this.size >= other.size
        }
      }
      else -> false
    }

  override fun toString() = "${if (unsigned) "u" else ""}int$size"
}

data class SolContract(val ref: SolContractDefinition) : SolType, Linearizable<SolContract> {
  override fun linearize(): List<SolContract> {
    return RecursionManager.doPreventingRecursion(ref, true) {
      CachedValuesManager.getCachedValue(ref) {
        CachedValueProvider.Result.create(super.linearize(), PsiModificationTracker.MODIFICATION_COUNT)
      }
    } ?: emptyList()
  }

  override fun linearizeParents(): List<SolContract> {
    return RecursionManager.doPreventingRecursion(ref, true) {
      CachedValuesManager.getCachedValue(ref) {
        CachedValueProvider.Result.create(super.linearizeParents(), PsiModificationTracker.MODIFICATION_COUNT)
      }
    } ?: emptyList()
  }

  override fun getParents(): List<SolContract> {
    return ref.supers
      .flatMap { it.reference?.multiResolve() ?: emptyList() }
      .filterIsInstance<SolContractDefinition>()
      .map { SolContract(it) }
      .reversed()
  }

  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolContract -> {
        other.ref == ref
          || other.ref.collectSupers.flatMap { SolResolver.resolveTypeNameUsingImports(it) }.contains(ref)
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

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SolStaticArray

      if (size != other.size) return false
      if (type != other.type) return false
      return true
    }

    override fun hashCode(): Int {
      return Objects.hash(size, type)
    }
  }

  class SolDynamicArray(type: SolType) : SolArray(type) {
    override fun isAssignableFrom(other: SolType): Boolean =
      other is SolDynamicArray && type == other.type

    override fun toString() = "$type[]"

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as SolDynamicArray

      if (type != other.type) return false
      return true
    }

    override fun hashCode(): Int {
      return type.hashCode()
    }

    override fun getBuiltinFunctions(project: Project): List<SolCallable> {
      return SolInternalTypeFactory.of(project).arrayType.ref
        .functionDefinitionList
        .map {
          val parameters = it.parseParameters()
            .map { pair -> pair.first to type }
          BuiltinCallable(parameters, it.parseReturnType(), it.callableName, it) }
    }
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

private const val INTERNAL_INDICATOR = "_sol1_s"

fun internalise(name: String): String = "$name$INTERNAL_INDICATOR"

fun isInternal(name: String): Boolean = name.endsWith(INTERNAL_INDICATOR)

fun deInternalise(name: String): String = when {
  name.endsWith(INTERNAL_INDICATOR) -> name.removeSuffix(INTERNAL_INDICATOR)
  else -> name
}

data class BuiltinCallable(
  private val parameters: List<Pair<String?, SolType>>,
  private val returnType: SolType,
  override val callableName: String?,
  override val resolvedElement: SolNamedElement?

) : SolCallable {
  override val callablePriority: Int
    get() = 1000
  override fun parseParameters(): List<Pair<String?, SolType>> = parameters
  override fun parseReturnType(): SolType = returnType
}
