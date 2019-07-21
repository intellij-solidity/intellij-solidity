package me.serce.solidity

import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.io.StreamUtil

fun <T> recursionGuard(key: Any, memoize: Boolean = true, block: Computable<T>): T? =
  RecursionManager.doPreventingRecursion(key, memoize, block)

fun <T> Sequence<T>.firstOrElse(el: T): T {
  return firstOrNull() ?: el
}

inline fun <reified R> Sequence<Any?>.firstInstance(): R {
  return first { it is R } as R
}

inline fun <reified R> Sequence<Any?>.firstInstanceOrNull(): R? {
  return firstOrNull { it is R } as? R
}

fun loadCodeSampleResource(ctx: Any, resource: String): String {
  val stream = ctx.javaClass.classLoader.getResourceAsStream(resource)
  // We need to convert line separators here, because IntelliJ always expects \n,
  // while on Windows the resource file will be read with \r\n as line separator.
  return StreamUtil.convertSeparators(StreamUtil.readText(stream, "UTF-8"))
}

fun <T> T?.wrap(): List<T> {
  return when (this) {
    null -> emptyList()
    else -> listOf(this)
  }
}
