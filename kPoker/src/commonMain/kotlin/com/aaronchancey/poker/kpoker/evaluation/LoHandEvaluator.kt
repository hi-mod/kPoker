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

        val comparator = Comparator<EvaluatedHand> { h1, h2 ->
            val v1 = h1.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()
            val v2 = h2.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()

            for (i in v1.indices) {
                if (i >= v2.size) break
                val diff = v1[i].compareTo(v2[i])
                if (diff != 0) return@Comparator diff
            }
            0
        }

        val best = combinations(cards, handSize)
            .map { evaluateLoHand(it) as EvaluatedHand }
            .filter { isQualifyingLow(it) }
            .minWithOrNull(comparator)

        return if (best != null) listOf(best) else emptyList()
    }

    override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> = findBestHand(holeCards + communityCards)

    private fun evaluateLoHand(cards: List<Card>): EvaluatedHand {
        // For lo hands, Ace is low (value 1)
        val sortedByLowValue = cards.sortedBy { if (it.rank == Rank.ACE) 1 else it.rank.value }
        return EvaluatedHand(HandRank.HIGH_CARD, sortedByLowValue)
    }

    private fun isQualifyingLow(hand: EvaluatedHand): Boolean {
        // 8-or-better qualification
        val values = hand.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }
        return values.distinct().size == 5 && values.max() <= 8
    }
}
