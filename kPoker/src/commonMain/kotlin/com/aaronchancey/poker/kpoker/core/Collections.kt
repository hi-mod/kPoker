package com.aaronchancey.poker.kpoker.core

object Collections {
    fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.isEmpty()) return emptyList()

        val first = list.first()
        val rest = list.drop(1)

        val withFirst = combinations(rest, k - 1).map { listOf(first) + it }
        val withoutFirst = combinations(rest, k)

        return withFirst + withoutFirst
    }
}
