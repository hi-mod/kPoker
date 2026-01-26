package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand

class OmahaHiLoHandEvaluator : HandEvaluator() {
    private val highEvaluator = OmahaHandEvaluator()
    private val loEvaluator = LoHandEvaluator()

    override fun evaluate(cards: List<Card>): EvaluatedHand {
        // Ambiguous, defaulting to High evaluation
        return highEvaluator.evaluate(cards)
    }

    override fun findBestHand(cards: List<Card>, handSize: Int): List<EvaluatedHand> {
        // Ambiguous, defaulting to High evaluation
        return highEvaluator.findBestHand(cards, handSize)
    }

    override fun evaluatePartial(cards: List<Card>): EvaluatedHand? {
        // Delegate to high evaluator which respects Omaha's 2-card rule
        return highEvaluator.evaluatePartial(cards)
    }

    override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> {
        val highHands = highEvaluator.findBestHand(holeCards, communityCards)

        // Manual 2+3 implementation for Low to ensure Omaha rules are followed
        var bestLoHand: EvaluatedHand? = null
        val holeCardCombos = combinations(holeCards, 2)
        val communityCombos = combinations(communityCards, 3)

        for (hole in holeCardCombos) {
            for (community in communityCombos) {
                val cards = hole + community
                // Check if qualifying low (8 or better, no pairs logic handled by LoEvaluator/check)
                val hand = loEvaluator.evaluate(cards)
                if (LoHandEvaluator.isQualifying(hand)) {
                    if (bestLoHand == null || LoHandEvaluator.compare(hand, bestLoHand) < 0) {
                        bestLoHand = hand
                    }
                }
            }
        }

        val loHands = if (bestLoHand != null) listOf(bestLoHand) else emptyList()

        return highHands + loHands
    }
}
