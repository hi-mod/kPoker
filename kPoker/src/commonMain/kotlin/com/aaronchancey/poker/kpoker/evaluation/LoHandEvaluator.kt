package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.core.Rank

class LoHandEvaluator : HandEvaluator {

    override fun evaluate(cards: List<Card>): EvaluatedHand {
        require(cards.size == 5) { "Lo hand evaluation requires exactly 5 cards" }
        return evaluateLoHand(cards)
    }

    override fun findBestHand(cards: List<Card>, handSize: Int): EvaluatedHand {
        require(cards.size >= handSize) { "Need at least $handSize cards" }
        return combinations(cards, handSize)
            .map { evaluateLoHand(it) }
            .filter { isQualifyingLow(it) }
            .minOrNull()
            ?: EvaluatedHand(HandRank.HIGH_CARD, emptyList()) // No qualifying low
    }

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
