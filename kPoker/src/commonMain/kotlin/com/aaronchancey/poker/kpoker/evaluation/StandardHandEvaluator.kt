package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.core.Rank

class StandardHandEvaluator : HandEvaluator() {

    override fun evaluate(cards: List<Card>): EvaluatedHand {
        require(cards.size == 5) { "Standard hand evaluation requires exactly 5 cards" }
        return evaluateFiveCardHand(cards)
    }

    override fun findBestHand(cards: List<Card>, handSize: Int): List<EvaluatedHand> {
        require(cards.size >= handSize) { "Need at least $handSize cards" }
        val best = combinations(cards, handSize).maxOfOrNull { evaluateFiveCardHand(it) }
            ?: throw IllegalStateException("No valid hand found")
        return listOf(best)
    }

    override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> = findBestHand(holeCards + communityCards)

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

        // Check flush before rank groups (flush beats straight, trips, two pair, pair)
        if (isFlush) return EvaluatedHand(HandRank.FLUSH, sorted)

        // Check straight before rank groups (straight beats trips, two pair, pair)
        if (isStraight) return EvaluatedHand(HandRank.STRAIGHT, sorted)

        // Delegate to shared rank-grouping logic for pairs/trips/quads/full house
        return evaluateByRankGroups(cards)
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
