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

enum class ContextType {
  SUPER,
  EXTERNAL,
  BUILTIN // for accessing a state variable within a builtin contract definition
}

enum class Usage {
  VARIABLE,
  CALLABLE
}

interface SolMember {
  fun getName(): String?
  fun parseType(): SolType
  fun resolveElement(): SolNamedElement?
  fun getPossibleUsage(contextType: ContextType): Usage?
}

interface SolType {
  fun isAssignableFrom(other: SolType): Boolean
  fun getMembers(project: Project): List<SolMember> {
    return emptyList()
  }
  val isBuiltin: Boolean
    get() = true
}

interface SolUserType : SolType {
  override val isBuiltin: Boolean
    get() = false

  val abiName: String
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

data class SolString(val length: Int) : SolPrimitiveType {
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolString

  override fun getMembers(project: Project) = getSdkMembers(SolInternalTypeFactory.of(project).stringType)

  override fun toString() = "string"

  companion object {
    fun inferType(stringLiteral: SolStringLiteral): SolString {
      return SolString(stringLiteral.text
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .length)
    }
  }
}

class SolAddress(val isPayable : Boolean) : SolPrimitiveType {
  private val toString = "address${if (isPayable) " payable" else ""}"
  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolAddress -> !this.isPayable || other.isPayable
      is SolContract -> other.ref.isPayable.let { !this.isPayable || it }
      else -> UINT_160.isAssignableFrom(other)
    }

  override fun getMembers(project: Project) = getSdkMembers(SolInternalTypeFactory.of(project).addressType)

  override fun toString() = toString

  companion object {
    val PAYABLE = SolAddress(true)
    val NON_PAYABLE = SolAddress(false)
  }
}

enum class NumericLiteralType {
  HEX, DECIMAL, SCIENTIFIC, ZERO
}

data class SolInteger(val unsigned: Boolean, val size: Int, val digitCount: Int? = null, val literalType: NumericLiteralType? = null) : SolNumeric {
  companion object {
    val UINT_160 = SolInteger(true, 160)
    val UINT_256 = SolInteger(true, 256)

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
      return inferIntegerType(numberLiteral.parseLiteral())
    }

    fun inferType(numberLiteral: SolHexLiteral): SolInteger {
      val withoutPrefix = numberLiteral.text.substring(3) //
        .removeSurrounding("\"") //
        .removeSurrounding("'") //
        .replace("_", "") // underscore is an allowed separator in hex literals
      val parse = LiteralParseResult(withoutPrefix.toBigInteger(16), NumericLiteralType.HEX, withoutPrefix.length)
      return inferIntegerType(parse)
    }

    private fun inferIntegerType(parseResult: LiteralParseResult): SolInteger {
      val value = parseResult.value
      if (value == BigInteger.ZERO) return SolInteger(true, 8, 1, NumericLiteralType.ZERO)
      val positive = value >= BigInteger.ZERO
      if (positive) {
        var shifts = 0
        var current = value
        while (current != BigInteger.ZERO) {
          shifts++
          current = current.shiftRight(8)
        }
        return SolInteger(positive, shifts * 8, parseResult.digitCount, parseResult.type)
      } else {
        var shifts = 1
        var current = value.abs().minus(BigInteger.ONE).shiftRight(7)
        while (current != BigInteger.ZERO) {
          shifts++
          current = current.shiftRight(8)
        }
        return SolInteger(positive, shifts * 8, parseResult.digitCount, parseResult.type)
      }
    }

    private class LiteralParseResult(val value: BigInteger, val type: NumericLiteralType, val digitCount: Int? = null)

    private fun SolNumberLiteral.parseLiteral(): LiteralParseResult  {
      this.decimalNumber?.let {
        return LiteralParseResult(it.text.replace("_", "").toBigInteger(), NumericLiteralType.DECIMAL)
      }
      this.hexNumber?.let {
        val withoutPrefix = it.text.removePrefix("0x")
        return LiteralParseResult(withoutPrefix.toBigInteger(16), NumericLiteralType.HEX, withoutPrefix.length)
      }
      this.scientificNumber?.let {
        return LiteralParseResult(it.text.replace("_", "").lowercase().toBigDecimal().toBigInteger(), NumericLiteralType.SCIENTIFIC)
      }
      //todo
      return LiteralParseResult(BigInteger.ZERO, NumericLiteralType.ZERO)
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

data class SolContract(val ref: SolContractDefinition, val builtin: Boolean = false) : SolUserType, Linearizable<SolContract> {
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
    .toList()
    .asReversed()
  }

  override fun isAssignableFrom(other: SolType): Boolean =
    when (other) {
      is SolContract -> {
        other.ref == ref
          || other.ref.collectSupers.flatMap { SolResolver.resolveTypeNameUsingImports(it) }.contains(ref)
      }
      else -> false
    }

  override fun getMembers(project: Project): List<SolMember> {
    return SolResolver.resolveContractMembers(ref, false)
  }

  override val isBuiltin get() = builtin
  override val abiName: String
    get() = "contract"

  override fun toString() = ref.name ?: ref.text ?: "$ref"
}

data class SolStruct(val ref: SolStructDefinition, val builtin : Boolean = false) : SolUserType, SolMember {
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolStruct && ref == other.ref

  override fun toString() = ref.name ?: ref.text ?: "$ref"

  override fun getMembers(project: Project): List<SolMember> {
    return ref.variableDeclarationList
      .map { SolStructVariableDeclaration(it) }
  }

  override val isBuiltin: Boolean
    get() = builtin

  override fun getName(): String? = ref.name

  override fun parseType(): SolType = this

  override fun resolveElement(): SolNamedElement? = ref

  override fun getPossibleUsage(contextType: ContextType): Usage? = null
  override val abiName: String
    get() = "struct"
}

data class SolStructVariableDeclaration(
  val ref: SolVariableDeclaration
) : SolMember {
  override fun getName(): String? = ref.name

  override fun parseType(): SolType = getSolType(ref.typeName)

  override fun resolveElement(): SolNamedElement? = ref

  override fun getPossibleUsage(contextType: ContextType) = Usage.VARIABLE
}

data class SolMemberConstructor<T>(val ref: T) : SolMember, SolCallable where T : SolCallable, T: SolNamedElement  {
  override val callablePriority: Int = 0

  override fun getName(): String? = ref.name

  override fun parseType(): SolType = ref.parseType()
  override fun parseParameters(): List<Pair<String?, SolType>> = ref.parseParameters()

  override fun resolveElement(): SolNamedElement = ref

  override fun getPossibleUsage(contextType: ContextType): Usage = Usage.CALLABLE

}

data class SolEnum(val ref: SolEnumDefinition) : SolUserType, SolMember {
  override val abiName: String
    get() = "enum"
  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolEnum && ref == other.ref

  override fun getName(): String? = ref.name

  override fun parseType(): SolType = this

  override fun resolveElement(): SolNamedElement? = ref

  override fun getPossibleUsage(contextType: ContextType): Usage? = null

  override fun toString() = ref.name ?: ref.text ?: "$ref"

  override fun getMembers(project: Project): List<SolMember> {
    return ref.enumValueList
  }
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

    override fun getMembers(project: Project) = SolInternalTypeFactory.of(project).arrayType.ref.stateVariableDeclarationList;
  }

  open class SolDynamicArray(type: SolType) : SolArray(type) {
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

    override fun getMembers(project: Project): List<SolMember> {
      return SolInternalTypeFactory.of(project).arrayType.ref.let {
             it.functionDefinitionList + it.stateVariableDeclarationList
           }
    }
  }
}

object SolBytes : SolArray.SolDynamicArray(SolFixedByte(1)) {
  private val concatFunction = BuiltinCallable(emptyList(), SolBytes, "concat", null)

  override fun isAssignableFrom(other: SolType): Boolean =
    other == SolBytes || other is SolString

  override fun getMembers(project: Project): List<SolMember> {
    return super.getMembers(project).toMutableList() + concatFunction
  }

  override fun toString() = "bytes"
}

data class SolFixedByte(val size: Int): SolPrimitiveType {
  override fun toString() = "byte$size"

  override fun isAssignableFrom(other: SolType): Boolean =
    other is SolFixedByte && other.size <= size

  companion object {

    val regex = "byte\\d*$".toRegex()
    fun parse(name: String): SolFixedByte {
      return if (name.startsWith("byte")) {
        SolFixedByte(name.substring(4).toInt())
      } else {
        throw java.lang.IllegalArgumentException("should start with 'byte'")
      }
    }
  }
}


data class SolFixedBytes(val size: Int): SolPrimitiveType {

  override fun toString() = "bytes$size"

  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolFixedBytes && other.size <= size ||
      other is SolInteger &&
      (other.literalType == NumericLiteralType.HEX && other.digitCount == size * 2 || other.literalType == NumericLiteralType.ZERO) ||
      other is SolString && (other.length == size || other.length == 0)
  }

  companion object {
    val regex = "bytes\\d*$".toRegex()
    fun parse(name: String): SolFixedBytes {
      return if (name.startsWith("bytes")) {
        SolFixedBytes(name.substring(5).toInt())
      } else {
        throw java.lang.IllegalArgumentException("should start with bytes")
      }
    }
  }
}

data class SolMetaType(val type: SolType): SolType {
  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolMetaType && this.type == other.type
  }

  override fun getMembers(project: Project): List<SolMember> {
    return getSdkMembers(SolInternalTypeFactory.of(project).metaType)
  }

  override fun toString(): String = "type($type)"
}

data class SolFunctionType(val ref: SolFunctionDefinition): SolType {
  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolFunctionType && this.ref.parseParameters() == other.ref.parseParameters() && this.ref.parseType() == other.ref.parseType() // todo state mutability of 'this' is more restrictive than the state mutability of 'other'
  }

  override fun getMembers(project: Project): List<SolMember> {
    return getReferenceTypeMembers(project, Usage.CALLABLE)
  }



  override fun toString(): String = "function(${ref.name})"
}

data class SolVariableType(val ref: SolStateVariableDeclaration): SolType {
  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolVariableType && this.ref.parseParameters() == other.ref.parseParameters() && this.ref.parseType() == other.ref.parseType()
  }

  override fun getMembers(project: Project): List<SolMember> {
    return getReferenceTypeMembers(project, Usage.VARIABLE)
  }

  override fun toString(): String = "variable(${ref.name})"
}

private fun getReferenceTypeMembers(project: Project, usage: Usage) =
    getSdkMembers(SolInternalTypeFactory.of(project).functionType).map {
      it.getName()?.let { name -> if (it is SolCallable && name.startsWith("__")) changeName(it, name.substring(2), usage) else it }
        ?: it
    }

data class SolUserDefinedValueTypeReferenceType(val ref: SolUserDefinedValueTypeDefinition): SolType {
  var elementaryType: SolType = getSolType(ref.elementaryTypeName)
  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolUserDefinedValueTypeReferenceType && this.elementaryType == other.elementaryType
  }

  override fun getMembers(project: Project): List<SolMember> {
    return listOf(BuiltinCallable(listOf("value" to this), elementaryType, "unwrap"), BuiltinCallable(listOf("value" to elementaryType), this, "wrap"))
  }

  override fun toString(): String = "ValueTypeReference(${ref.name})"
}

data class SolUserDefinedValueTypeType(val ref: SolUserDefinedValueTypeDefinition): SolType {
  var elementaryType: SolType = getSolType(ref.elementaryTypeName)
  override fun isAssignableFrom(other: SolType): Boolean {
    return other is SolUserDefinedValueTypeType && this.elementaryType == other.elementaryType
  }

  override fun toString(): String = "ValueType(${ref.name})"
}




data class SolFunctionReference(val ref: SolFunctionDefinition): SolMember {
  override fun getName(): String? = ref.name

  override fun parseType(): SolType {
    return SolFunctionType(ref)
  }

  override fun resolveElement(): SolNamedElement? = ref

  override fun getPossibleUsage(contextType: ContextType): Usage? = Usage.VARIABLE

}

data class SolFunctionTypeType(val ref: SolFunctionTypeName): SolType {
  override fun isAssignableFrom(other: SolType): Boolean = when (other) {
    is SolFunctionTypeType -> this.ref == other.ref
    is SolFunctionType -> {
      val func = other.ref
      this.ref.params.zip(func.parameters).all { getSolType(it.first.typeName).isAssignableFrom(getSolType(it.second.typeName)) } &&
        this.ref.returns.isAssignableFrom(func.parseType()) &&
        this.ref.mutability == other.ref.mutability && this.ref.visibility == other.ref.visibility
    }
    else -> false
  }

  override fun toString(): String {
    return "functionType(${ref.params.joinToString {it.text}}) ${ref.returns.takeIf { it != SolUnknown }?.let { " returns ($it)" } ?: ""}"
  }
}

fun SolParameterList?.parseType(): SolType = when (this) {
  null -> SolUnknown
  else -> parameterDefList.let {
    when (it.size) {
      1 -> getSolType(it[0].typeName)
      else -> SolTuple(it.map { def -> getSolType(def.typeName) })
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

class BuiltinType(
  private val name: String,
  private val members: List<SolMember>
) : SolType {
  override fun isAssignableFrom(other: SolType): Boolean = false
  override fun getMembers(project: Project): List<SolMember> = members
  override fun toString(): String = name
}

data class BuiltinCallable(
  private val parameters: List<Pair<String?, SolType>>,
  private val returnType: SolType,
  private val memberName: String?,
  private val resolvedElement: SolNamedElement? = null,
  private val possibleUsage: Usage = Usage.CALLABLE
) : SolCallable, SolMember {
  override val callablePriority: Int
    get() = 1000
  override fun parseParameters(): List<Pair<String?, SolType>> = parameters
  override fun parseType(): SolType = returnType
  override fun resolveElement(): SolNamedElement? = resolvedElement
  override fun getName(): String? = memberName
  override fun getPossibleUsage(contextType: ContextType) = possibleUsage
}




private fun getSdkMembers(solContract: SolContract): List<SolMember> {
    return solContract.ref.let { it.functionDefinitionList + it.stateVariableDeclarationList }
}

private fun changeName(it: SolCallable, newName: String, callable: Usage): SolMember {
  return BuiltinCallable(it.parseParameters(), it.parseType(), newName)
}
