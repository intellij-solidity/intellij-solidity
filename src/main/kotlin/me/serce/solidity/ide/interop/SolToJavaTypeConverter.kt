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
    return when (type) {
      is SolElementaryTypeName -> ({
        val numberType = type.numberType
        if (numberType != null) when {
          numberType.byteNumType != null -> ByteArray::class
          numberType.intNumType != null -> processIntType(numberType.intNumType, true)
          numberType.uIntNumType != null -> processIntType(numberType.uIntNumType, false)
          numberType.fixedNumType != null || numberType.uFixedNumType != null -> BigDecimal::class
          else -> default
        } else when (type.text) {
          "string" -> String::class
          "address" -> ByteArray::class
          "bool" -> Boolean::class
          "var" -> default
          else -> default
        }
      })()
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

