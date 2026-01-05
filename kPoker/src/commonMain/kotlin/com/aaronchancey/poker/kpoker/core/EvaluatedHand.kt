package com.aaronchancey.poker.kpoker.core

import kotlinx.serialization.Serializable

@Serializable
data class EvaluatedHand(
    val rank: HandRank,
    val cards: List<Card>,
    val kickers: List<Card> = emptyList(),
) : Comparable<EvaluatedHand> {

    override fun compareTo(other: EvaluatedHand): Int {
        val rankComparison = rank.rank.compareTo(other.rank.rank)
        if (rankComparison != 0) return rankComparison

        // Compare primary hand cards
        val primaryComparison = compareCardLists(cards, other.cards)
        if (primaryComparison != 0) return primaryComparison

        // Compare kickers
        return compareCardLists(kickers, other.kickers)
    }

    private fun compareCardLists(a: List<Card>, b: List<Card>): Int {
        val sortedA = a.sortedByDescending { it.rank.value }
        val sortedB = b.sortedByDescending { it.rank.value }

        for (i in 0 until minOf(sortedA.size, sortedB.size)) {
            val comp = sortedA[i].rank.value.compareTo(sortedB[i].rank.value)
            if (comp != 0) return comp
        }
        return 0
    }

    fun description(): String = buildString {
        append(rank.displayName)
        if (cards.isNotEmpty()) {
            append(": ")
            append(cards.joinToString(" ") { it.toString() })
        }
    }
}
