package me.serce.solidity.ide.interop

import com.intellij.psi.PsiElement
import me.serce.solidity.lang.psi.*
import java.lang.reflect.Array
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

object SolToJavaTypeConverter {
  val default = Any::class

  fun convert(type: SolTypeName): String {
    val clazz = getClass(type)
    return clazz.java.canonicalName
  }

  private fun getClass(type: SolTypeName): KClass<out Any> {
    // todo support other solidity types once they get introduced
    return when (type) {
      is SolElementaryTypeName -> {
        val numberType = type.numberType ?: return default
        if (numberType.byteNumType != null) return ByteArray::class
        if (numberType.intNumType != null) return processIntType(numberType.intNumType, true)
        if (numberType.uIntNumType != null) return processIntType(numberType.uIntNumType, false)
        if (numberType.fixedNumType != null || numberType.uFixedNumType != null) return BigDecimal::class
        return default
      }
      is SolBytesArrayTypeName -> ByteArray::class
      is SolFunctionTypeName -> ByteArray::class
      is SolArrayTypeName -> {
        SolToJavaTypeConverter.arrayType(getClass(type.typeName))
      }
      else -> default
    }
  }

  private fun arrayType(tt: KClass<out Any>): KClass<out Any> {
    return Array.newInstance(tt.javaObjectType, 0)::class
  }

  private val intBitsRegex = Regex("u?int(\\d{0,3})")

  private fun processIntType(type: PsiElement?, singed: Boolean): KClass<out Any> {
    val text = type?.text ?: return default
    val find = intBitsRegex.find(text) ?: return default
    val bitsStr = find.groupValues[1]
    val bits = if (bitsStr.isEmpty()) 256 else bitsStr.toInt()
    return when (bits) {
      in 1..31 -> Int::class
      32 -> if (singed) Int::class else Long::class
      in 33..63 -> Long::class
      64 -> if (singed) Long::class else BigInteger::class
      else -> BigInteger::class
    }
  }


}

