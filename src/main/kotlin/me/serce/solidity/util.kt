package me.serce.solidity

import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.RecursionManager

fun <T> recursionGuard(key: Any, memoize: Boolean = true, block: Computable<T>): T? =
  RecursionManager.doPreventingRecursion(key, memoize, block)


fun <T> Sequence<T>.firstOrElse(el: T): T {
  return firstOrNull() ?: el
}

inline fun <reified R> Sequence<Any?>.firstInstance(): R {
  return first { it is R} as R
}
