package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ActionType
import com.aaronchancey.poker.kpoker.equity.ActionEv
import com.aaronchancey.poker.kpoker.equity.ActionEvCalculator
import com.aaronchancey.poker.kpoker.equity.EquityCalculator
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Computes action EVs for the hero by running Monte Carlo equity simulation
 * and feeding results into [ActionEvCalculator].
 *
 * This is a pure computation provider — callers are responsible for deciding
 * when to invoke it and how to wire the result into UI state.
 */
class ActionEvProvider {

    /**
     * Calculates action EV for the hero's current situation.
     *
     * Runs the equity simulation on [Dispatchers.Default] to avoid blocking the main thread.
     *
     * @param gameState Current game state
     * @param playerId The hero's player ID
     * @param availableActions The hero's current action request, or null if not their turn
     * @return [ActionEv] with equity and EV values, or null if not enough info to calculate
     */
    suspend fun getActionEv(
        gameState: GameState?,
        playerId: PlayerId?,
        availableActions: ActionRequest?,
    ): ActionEv? {
        if (gameState == null || playerId == null) return null

        val holeCards = gameState.activePlayers
            .firstOrNull { it.player.id == playerId }
            ?.holeCards
            ?: return null

        if (holeCards.isEmpty()) return null

        val opponentCount = gameState.playersInHand.size - 1
        if (opponentCount <= 0) return null

        val communityCards = gameState.communityCards

        val equity = withContext(Dispatchers.Default) {
            EquityCalculator.calculateEquity(
                heroHoleCards = holeCards,
                communityCards = communityCards,
                opponentCount = opponentCount,
                variant = gameState.variant,
            )
        }

        val potSize = gameState.effectivePot
        val validActions = availableActions?.validActions.orEmpty()
        val amountToCall = availableActions?.amountToCall ?: 0.0

        return ActionEvCalculator.calculate(
            equity = equity,
            potSize = potSize,
            amountToCall = amountToCall,
            canCheck = ActionType.CHECK in validActions,
            canCall = ActionType.CALL in validActions,
        )
    }
}
