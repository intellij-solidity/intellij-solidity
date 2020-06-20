package me.serce.solidity.ide.navigation

import com.intellij.openapi.util.Condition
import com.intellij.util.*

fun <U, V> Query<U>.mapQuery(f: (U) -> V) = object : AbstractQuery<V>() {
  override fun processResults(consumer: Processor<in V>): Boolean {
    return this@mapQuery.forEach(Processor<U> { t -> consumer.process(f(t)) })
  }
}

inline fun <reified V : Any> Query<*>.filterIsInstanceQuery(): Query<V> = InstanceofQuery(this, V::class.java)

fun <U> Query<U>.filterQuery(condition: Condition<U>): Query<U> = FilteredQuery(this, condition)
