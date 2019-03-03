package me.serce.solidity.lang.psi.impl

interface Linearizable<T : Linearizable<T>> {

  fun getParents(): List<T>

  @Suppress("UNCHECKED_CAST")
  fun linearize(): List<T> {
    return listOf(this as T)  + linearizeParents()
  }

  fun linearizeParents(): List<T> {
    val parents = getParents()
    return (parents.map { it.linearize() } + listOf(parents)).merge()
  }
}

fun <T> List<List<T>>.merge(): List<T> {
  val result = mutableListOf<T>()
  var lists = this.filter { !it.isEmpty() }
  while (!lists.isEmpty()) {
    val next = lists.nextForMerge()
    if (next != null) {
      result.add(next)
      lists = lists.map { it.filter { i -> i != next } }.filter { !it.isEmpty() }
    } else {
      throw LinearizationImpossibleException("result: $result lists: $lists source: $this")
    }
  }
  return result
}

class LinearizationImpossibleException(message: String) : Exception(message)

fun <T> List<List<T>>.nextForMerge(): T? {
  for (list in this) {
    val head = list.firstOrNull()
    val foundInTail = this.any { otherList ->
      otherList != list && otherList.lastIndexOf(head) > 0
    }
    if (!foundInTail)
      return head
  }
  return null
}
