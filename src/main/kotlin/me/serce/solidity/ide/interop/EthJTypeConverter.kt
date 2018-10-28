package me.serce.solidity.ide.interop

import java.lang.reflect.Array
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass

object EthJTypeConverter {
  val default = Any::class

  fun convert(type: String?, lax: Boolean = false): String {
    val clazz = if (type != null) getClass(type, lax) else default
    return clazz.java.canonicalName
  }

  private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

  private fun getClass(type: String, lax: Boolean): KClass<out Any> {
    return when (type) {
          "string" -> String::class
          "address" -> ByteArray::class
          "bool" -> Boolean::class
          "var" -> default
      "function" -> ByteArray::class
      in "bytes" -> ByteArray::class
      in arrayRegex -> arrayType(getClass(type.replaceFirst("[]", ""), lax))
      in intBitsRegex -> if (lax) BigInteger::class else processIntType(type)
      in fixedBitsRegex -> BigDecimal::class
      else -> default
    }
  }

  private fun arrayType(tt: KClass<out Any>): KClass<out Any> {
    return Array.newInstance(tt.javaObjectType, 0)::class
  }

  private val intBitsRegex = Regex("u?int(\\d{0,3})")
  private val fixedBitsRegex = Regex("u?fixed(\\d{0,3})x?(\\d{0,2})")
  private val arrayRegex = Regex("\\w+(\\[])+")

  private fun processIntType(text: String): KClass<out Any> {
    val find = intBitsRegex.find(text) ?: return default
    val bitsStr = find.groupValues[1]
    val signed = !text.startsWith("u")
    val bits = if (bitsStr.isEmpty()) 256 else bitsStr.toInt()
    return when (bits) {
      in 1..31 -> Int::class
      32 -> if (signed) Int::class else Long::class
      in 33..63 -> Long::class
      64 -> if (signed) Long::class else BigInteger::class
      else -> BigInteger::class
    }
  }


}
