package com.aaronchancey.poker.kpoker.core

object Collections {
    fun <T> combinations(list: List<T>, k: Int): Sequence<List<T>> = sequence {
        println("Generating combinations of $k from list of size ${list.size}")
        val n = list.size
        val indices = IntArray(k)

        suspend fun SequenceScope<List<T>>.generate(done: Int, begin: Int) {
            for (i in begin until n) {
                indices[done] = i
                if (done == k - 1) {
                    val combination = ArrayList<T>(k)
                    for (j in 0 until k) {
                        combination.add(list[indices[j]])
                    }
                    yield(combination)
                } else {
                    generate(done + 1, i + 1)
                }
            }
        }

        if (k in 1..n) {
            generate(0, 0)
        } else if (k == 0) {
            yield(emptyList())
        }
    }
}
