package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.evaluation.HandEvaluatorFactory
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.player.PlayerId

/**
 * Provides human-readable hand descriptions for the current player.
 *
 * Extracts hand evaluation logic from the ViewModel to improve testability
 * and keep the ViewModel focused on state coordination.
 */
class HandDescriptionProvider {

    /**
     * Generates a human-readable description of the player's current hand.
     *
     * Behavior varies by game phase:
     * - **Pre-flop** (< 3 community cards): Evaluates hole cards only using [evaluatePartial]
     * - **Post-flop** (≥ 3 community cards): Evaluates best 5-card hand from available cards
     *
     * @param gameState Current game state containing community cards and player hands
     * @param playerId The player whose hand to describe
     * @return Hand description (e.g., "Pair of Aces") or empty string if unavailable
     */
    fun getHandDescription(gameState: GameState?, playerId: PlayerId?): String {
        if (gameState == null || playerId == null) return ""

        val communityCards = gameState.communityCards
        val holeCards = gameState.activePlayers
            .firstOrNull { it.player.id == playerId }
            ?.holeCards
            ?: return ""

        if (holeCards.isEmpty()) return ""

        return try {
            val variant = gameState.variant
            val evaluator = HandEvaluatorFactory.getEvaluator(variant)

            if (communityCards.size >= 3) {
                // Post-flop: full hand evaluation
                val bestHands = when (variant) {
                    GameVariant.TEXAS_HOLDEM -> {
                        val allCards = holeCards + communityCards
                        if (allCards.size >= 5) {
                            evaluator.findBestHand(allCards, 5)
                        } else {
                            emptyList()
                        }
                    }

                    else -> {
                        // Omaha variants: must use exactly 2 hole cards + 3 community cards
                        if (holeCards.size >= 2 && communityCards.size >= 3) {
                            evaluator.findBestHand(holeCards, communityCards)
                        } else {
                            emptyList()
                        }
                    }
                }
                bestHands.joinToString(", ") { it.description() }
            } else {
                // Pre-flop: partial evaluation of hole cards only
                evaluator.evaluatePartial(holeCards)?.description() ?: ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
