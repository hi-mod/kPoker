package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand

class OmahaHandEvaluator(
    private val baseEvaluator: HandEvaluator = StandardHandEvaluator(),
) : HandEvaluator {

    override fun evaluate(cards: List<Card>): EvaluatedHand = baseEvaluator.evaluate(cards)

    override fun findBestHand(cards: List<Card>, handSize: Int): List<EvaluatedHand> {
        // This is ambiguous for Omaha if we don't know which are hole cards.
        return baseEvaluator.findBestHand(cards, handSize)
    }

    override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> {
        var bestHand: EvaluatedHand? = null

        // Must use exactly 2 hole cards
        val holeCardCombos = combinations(holeCards, 2)
        // Must use exactly 3 community cards
        val communityCombos = combinations(communityCards, 3)

        for (hole in holeCardCombos) {
            for (community in communityCombos) {
                val hand = baseEvaluator.evaluate(hole + community)
                if (bestHand == null || hand > bestHand) {
                    bestHand = hand
                }
            }
        }

        return if (bestHand != null) listOf(bestHand) else emptyList()
    }
}
