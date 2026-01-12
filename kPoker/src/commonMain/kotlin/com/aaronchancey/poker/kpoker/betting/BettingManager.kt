package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import kotlinx.serialization.Serializable

@Serializable
enum class BettingRoundType {
    PRE_FLOP,
    FLOP,
    TURN,
    RIVER,
}

@Serializable
data class BettingRound(
    val type: BettingRoundType,
    val currentBet: ChipAmount = 0.0,
    val minimumRaise: ChipAmount = 0.0,
    val lastRaiseAmount: ChipAmount = 0.0,
    val actingPlayerIndex: Int = 0,
    val lastAggressorId: PlayerId? = null,
    val actions: List<Action> = emptyList(),
    val isComplete: Boolean = false,
)

@Serializable
data class BettingStructure(
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val ante: ChipAmount = 0.0,
    val bringIn: ChipAmount = 0.0,
    val minBet: ChipAmount = bigBlind,
    val maxBet: ChipAmount? = null, // null = no limit
    val maxRaises: Int? = null, // null = unlimited
) {
    val isNoLimit: Boolean get() = maxBet == null
    val isPotLimit: Boolean get() = false // Determined by game logic
    val isFixedLimit: Boolean get() = maxBet != null && maxRaises != null

    companion object {
        fun noLimit(smallBlind: ChipAmount, bigBlind: ChipAmount, ante: ChipAmount = 0.0) = BettingStructure(smallBlind, bigBlind, ante)

        fun potLimit(smallBlind: ChipAmount, bigBlind: ChipAmount, ante: ChipAmount = 0.0) = BettingStructure(smallBlind, bigBlind, ante) // Pot limit enforced in game logic

        fun fixedLimit(smallBlind: ChipAmount, bigBlind: ChipAmount, ante: ChipAmount = 0.0, maxRaises: Int = 4) = BettingStructure(
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            minBet = bigBlind,
            maxBet = bigBlind,
            maxRaises = maxRaises,
        )
    }
}

class BettingManager(
    private val structure: BettingStructure,
) {
    fun getValidActions(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
        isPotLimit: Boolean = false,
    ): ActionRequest {
        val minBet = if (currentBet > 0.0) currentBet else structure.minBet

        val amountToCall = currentBet - playerState.currentBet

        val validActions = buildSet {
            // Always can fold (unless no bet to call)
            if (amountToCall > 0.0) {
                add(ActionType.FOLD)
            }

            // Check if no bet to call
            if (amountToCall == 0.0) {
                add(ActionType.CHECK)
            }

            // Call if there's a bet and player has chips
            if (amountToCall > 0 && playerState.chips > 0) {
                add(ActionType.CALL)
            }

            // Bet if no current bet
            if (currentBet == 0.0 && playerState.chips >= structure.minBet) {
                add(ActionType.BET)
            }

            // Raise if there's a bet and player has enough chips
            if (currentBet > 0 && playerState.chips > amountToCall) {
                add(ActionType.RAISE)
            }

            // All-in is always available if player has chips
            if (playerState.chips > 0) {
                add(ActionType.ALL_IN)
            }
        }

        val maxBet = if (isPotLimit) {
            potSize + (currentBet * 2) - playerState.currentBet
        } else {
            structure.maxBet ?: playerState.chips
        }

        return ActionRequest(
            playerId = playerState.player.id,
            validActions = validActions,
            minimumBet = minBet,
            minimumRaise = minOf(minRaise, playerState.chips),
            maximumBet = minOf(maxBet, playerState.chips),
            amountToCall = minOf(amountToCall, playerState.chips),
        )
    }

    fun validateAction(
        action: Action,
        playerState: PlayerState,
        currentBet: ChipAmount,
        minRaise: ChipAmount,
    ): Boolean = when (action) {
        is Action.Fold -> true

        is Action.Check -> currentBet == playerState.currentBet

        is Action.Call -> {
            val toCall = currentBet - playerState.currentBet
            val maxCall = minOf(toCall, playerState.chips)
            action.amount >= maxCall - EPSILON && action.amount <= maxCall + EPSILON
        }

        is Action.Bet -> {
            currentBet == 0.0 &&
                action.amount >= structure.minBet - EPSILON &&
                action.amount <= playerState.chips + EPSILON
        }

        is Action.Raise -> {
            val toCall = currentBet - playerState.currentBet
            action.amount >= minRaise - EPSILON &&
                action.totalBet >= currentBet + minRaise - EPSILON &&
                action.amount + toCall <= playerState.chips + EPSILON
        }

        is Action.AllIn -> action.amount >= playerState.chips - EPSILON && action.amount <= playerState.chips + EPSILON

        is Action.PostBlind -> action.amount <= playerState.chips + EPSILON
    }

    companion object {
        private const val EPSILON = 0.000001
    }
}
