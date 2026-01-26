package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.core.Rank

class LoHandEvaluator : HandEvaluator() {

    override fun evaluate(cards: List<Card>): EvaluatedHand {
        require(cards.size == 5) { "Lo hand evaluation requires exactly 5 cards" }
        return evaluateLoHand(cards)
    }

    override fun findBestHand(cards: List<Card>, handSize: Int): List<EvaluatedHand> {
        require(cards.size >= handSize) { "Need at least $handSize cards" }

        val best = combinations(cards, handSize)
            .map { evaluateLoHand(it) }
            .filter { isQualifying(it) }
            .minWithOrNull { a, b -> compare(a, b) }

        return if (best != null) listOf(best) else emptyList()
    }

    override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> = findBestHand(holeCards + communityCards)

    private fun evaluateLoHand(cards: List<Card>): EvaluatedHand {
        // For lo hands, Ace is low (value 1)
        val sortedByLowValue = cards.sortedBy { if (it.rank == Rank.ACE) 1 else it.rank.value }
        return EvaluatedHand(HandRank.HIGH_CARD, sortedByLowValue)
    }

    companion object {
        fun isQualifying(hand: EvaluatedHand): Boolean {
            // 8-or-better qualification
            val values = hand.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }
            return values.distinct().size == 5 && (values.maxOrNull() ?: 100) <= 8
        }

        fun compare(a: EvaluatedHand, b: EvaluatedHand): Int {
            val aValues = a.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()
            val bValues = b.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()

            for (i in aValues.indices) {
                if (i >= bValues.size) break
                val diff = aValues[i].compareTo(bValues[i])
                if (diff != 0) return diff
            }
            return 0
        }
    }
}
