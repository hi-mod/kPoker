package com.aaronchancey.poker.kpoker.equity

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Deck
import com.aaronchancey.poker.kpoker.evaluation.HandEvaluatorFactory
import com.aaronchancey.poker.kpoker.game.GameVariant

/**
 * Monte Carlo equity calculator for estimating hand win probability.
 *
 * Simulates random opponent hands and remaining community cards to estimate
 * the hero's equity (probability of winning at showdown). Opponents are always
 * modeled as random hands — no range or behavioral modeling.
 *
 * This is a pure computation with no side effects. Callers are responsible
 * for running it off the main thread.
 */
object EquityCalculator {

    /**
     * Estimates hero's showdown equity via Monte Carlo simulation.
     *
     * @param heroHoleCards The hero's hole cards
     * @param communityCards Current community cards (0-5)
     * @param opponentCount Number of opponents to simulate
     * @param variant The poker variant (determines evaluator and hole card count)
     * @param iterations Number of Monte Carlo iterations
     * @return Win equity as a Double from 0.0 (never wins) to 1.0 (always wins).
     *         Ties are counted as fractional wins (1/N for N-way tie).
     */
    fun calculateEquity(
        heroHoleCards: List<Card>,
        communityCards: List<Card>,
        opponentCount: Int,
        variant: GameVariant,
        iterations: Int = 1000,
    ): Double {
        require(heroHoleCards.isNotEmpty()) { "Hero must have hole cards" }
        require(opponentCount > 0) { "Must have at least one opponent" }

        val evaluator = HandEvaluatorFactory.getEvaluator(variant)
        val holeCardCount = heroHoleCards.size
        val communityCardsNeeded = 5 - communityCards.size
        val knownCards = heroHoleCards + communityCards

        var totalEquity = 0.0

        repeat(iterations) {
            val deck = Deck.standard()
            deck.removeCards(knownCards)

            val simCommunity = communityCards + deck.deal(communityCardsNeeded)
            val opponentHands = (1..opponentCount).map { deck.deal(holeCardCount) }

            val heroHand = evaluator.findBestHand(heroHoleCards, simCommunity).first()
            val opponentBestHands = opponentHands.map { hand ->
                evaluator.findBestHand(hand, simCommunity).first()
            }

            val bestOpponent = opponentBestHands.max()
            totalEquity += when {
                heroHand > bestOpponent -> 1.0
                heroHand < bestOpponent -> 0.0
                else -> {
                    val tiedCount = 1 + opponentBestHands.count { it.compareTo(heroHand) == 0 }
                    1.0 / tiedCount
                }
            }
        }

        return totalEquity / iterations
    }
}
