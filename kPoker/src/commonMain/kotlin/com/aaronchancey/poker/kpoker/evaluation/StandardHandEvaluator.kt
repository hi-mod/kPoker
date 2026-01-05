package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.core.Rank

class StandardHandEvaluator : HandEvaluator {

    override fun evaluate(cards: List<Card>): EvaluatedHand {
        require(cards.size == 5) { "Standard hand evaluation requires exactly 5 cards" }
        return evaluateFiveCardHand(cards)
    }

    override fun findBestHand(cards: List<Card>, handSize: Int): EvaluatedHand {
        require(cards.size >= handSize) { "Need at least $handSize cards" }
        return combinations(cards, handSize)
            .map { evaluateFiveCardHand(it) }
            .maxOrNull()
            ?: throw IllegalStateException("No valid hand found")
    }

    private fun evaluateFiveCardHand(cards: List<Card>): EvaluatedHand {
        val sorted = cards.sortedByDescending { it.rank.value }
        val isFlush = cards.map { it.suit }.distinct().size == 1
        val isStraight = isStraight(sorted)

        // Check for straight flush / royal flush
        if (isFlush && isStraight) {
            return if (sorted[0].rank == Rank.ACE && sorted[1].rank == Rank.KING) {
                EvaluatedHand(HandRank.ROYAL_FLUSH, sorted)
            } else {
                EvaluatedHand(HandRank.STRAIGHT_FLUSH, sorted)
            }
        }

        val rankGroups = cards.groupBy { it.rank }.values.sortedByDescending { it.size }
        val groupSizes = rankGroups.map { it.size }

        return when {
            groupSizes[0] == 4 -> {
                EvaluatedHand(
                    HandRank.FOUR_OF_A_KIND,
                    rankGroups[0],
                    rankGroups.drop(1).flatten(),
                )
            }

            groupSizes[0] == 3 && groupSizes[1] == 2 -> {
                EvaluatedHand(
                    HandRank.FULL_HOUSE,
                    rankGroups[0] + rankGroups[1],
                )
            }

            isFlush -> EvaluatedHand(HandRank.FLUSH, sorted)

            isStraight -> EvaluatedHand(HandRank.STRAIGHT, sorted)

            groupSizes[0] == 3 -> {
                EvaluatedHand(
                    HandRank.THREE_OF_A_KIND,
                    rankGroups[0],
                    rankGroups.drop(1).flatten().sortedByDescending { it.rank.value },
                )
            }

            groupSizes[0] == 2 && groupSizes[1] == 2 -> {
                val pairs = rankGroups.take(2).sortedByDescending { it[0].rank.value }
                EvaluatedHand(
                    HandRank.TWO_PAIR,
                    pairs.flatten(),
                    rankGroups.drop(2).flatten(),
                )
            }

            groupSizes[0] == 2 -> {
                EvaluatedHand(
                    HandRank.ONE_PAIR,
                    rankGroups[0],
                    rankGroups.drop(1).flatten().sortedByDescending { it.rank.value },
                )
            }

            else -> EvaluatedHand(HandRank.HIGH_CARD, sorted.take(1), sorted.drop(1))
        }
    }

    private fun isStraight(sorted: List<Card>): Boolean {
        val values = sorted.map { it.rank.value }

        // Check regular straight
        val isRegularStraight = values.zipWithNext().all { (a, b) -> a - b == 1 }
        if (isRegularStraight) return true

        // Check wheel (A-2-3-4-5)
        val wheelValues = listOf(14, 5, 4, 3, 2)
        return values == wheelValues
    }
}
