package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank

abstract class HandEvaluator {
    abstract fun evaluate(cards: List<Card>): EvaluatedHand
    abstract fun findBestHand(cards: List<Card>, handSize: Int = 5): List<EvaluatedHand>
    abstract fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand>

    /**
     * Evaluates a partial hand (2-4 cards) for pre-flop/early street UI feedback.
     *
     * With fewer than 5 cards, only certain hand ranks are achievable:
     * - 2 cards: HIGH_CARD or ONE_PAIR
     * - 3 cards: HIGH_CARD, ONE_PAIR, or THREE_OF_A_KIND
     * - 4 cards: All of the above plus TWO_PAIR or FOUR_OF_A_KIND
     *
     * @param cards The cards to evaluate (must be 1-4 cards)
     * @return The best possible hand rank achievable with these cards, or null if no cards
     */
    open fun evaluatePartial(cards: List<Card>): EvaluatedHand? {
        if (cards.isEmpty()) return null
        if (cards.size >= 5) return evaluate(cards.take(5))
        return evaluateByRankGroups(cards)
    }

    /**
     * Evaluates cards based purely on rank groupings (pairs, trips, quads).
     * Does not detect flushes or straights - use this for partial hands or as a
     * fallback after checking flush/straight in 5-card evaluation.
     */
    protected fun evaluateByRankGroups(cards: List<Card>): EvaluatedHand {
        val rankGroups = cards.groupBy { it.rank }.values
            .sortedWith(compareByDescending<List<Card>> { it.size }.thenByDescending { it[0].rank.value })
        val groupSizes = rankGroups.map { it.size }

        return when {
            groupSizes[0] == 4 -> EvaluatedHand(
                rank = HandRank.FOUR_OF_A_KIND,
                cards = rankGroups[0],
                kickers = rankGroups.drop(1).flatten(),
            )

            groupSizes[0] == 3 && groupSizes.getOrNull(1) == 2 -> EvaluatedHand(
                rank = HandRank.FULL_HOUSE,
                cards = rankGroups[0] + rankGroups[1],
            )

            groupSizes[0] == 3 -> EvaluatedHand(
                rank = HandRank.THREE_OF_A_KIND,
                cards = rankGroups[0],
                kickers = rankGroups.drop(1).flatten().sortedByDescending { it.rank.value },
            )

            groupSizes[0] == 2 && groupSizes.getOrNull(1) == 2 -> EvaluatedHand(
                rank = HandRank.TWO_PAIR,
                cards = rankGroups[0] + rankGroups[1],
                kickers = rankGroups.drop(2).flatten(),
            )

            groupSizes[0] == 2 -> EvaluatedHand(
                rank = HandRank.ONE_PAIR,
                cards = rankGroups[0],
                kickers = rankGroups.drop(1).flatten().sortedByDescending { it.rank.value },
            )

            else -> {
                val sorted = cards.sortedByDescending { it.rank.value }
                EvaluatedHand(
                    rank = HandRank.HIGH_CARD,
                    cards = sorted.take(1),
                    kickers = sorted.drop(1),
                )
            }
        }
    }
}
