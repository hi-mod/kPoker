package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState

class BettingManager(
    private val structure: BettingStructure,
    private val bettingRules: BettingRules = when (structure.bettingType) {
        BettingType.POT_LIMIT -> PotLimitRules(structure)
        BettingType.NO_LIMIT -> NoLimitRules(structure)
        BettingType.LIMIT -> LimitRules(structure)
    },
) {
    fun getValidActions(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): ActionRequest {
        val betLimits = bettingRules.calculateBetLimits(playerState, currentBet, potSize, minRaise)

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

        return ActionRequest(
            playerId = playerState.player.id,
            validActions = validActions,
            minimumDenomination = structure.minDenomination,
            minimumBet = betLimits.minBet,
            minimumRaise = minOf(minRaise, playerState.chips),
            maximumBet = minOf(betLimits.maxBet, playerState.chips),
            amountToCall = minOf(amountToCall, playerState.chips),
        )
    }

    fun validateAction(
        action: Action,
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): Boolean {
        val betLimits = bettingRules.calculateBetLimits(playerState, currentBet, potSize, minRaise)

        return when (action) {
            is Action.Fold -> true

            is Action.Check -> currentBet == playerState.currentBet

            is Action.Call -> {
                val toCall = currentBet - playerState.currentBet
                val maxCall = minOf(toCall, playerState.chips)
                action.amount >= maxCall - EPSILON && action.amount <= maxCall + EPSILON
            }

            is Action.Bet -> {
                val isValidDenomination = isValidDenomination(action.amount)
                currentBet == 0.0 &&
                    bettingRules.validateBetAmount(action.amount, betLimits, isRaise = false) &&
                    isValidDenomination
            }

            is Action.Raise -> {
                val toCall = currentBet - playerState.currentBet
                val isValidDenomination = isValidDenomination(action.amount)
                val totalBetAmount = action.amount + toCall
                action.amount >= minRaise - EPSILON &&
                    action.totalBet >= currentBet + minRaise - EPSILON &&
                    bettingRules.validateBetAmount(totalBetAmount, betLimits, isRaise = true) &&
                    isValidDenomination
            }

            is Action.AllIn -> action.amount >= playerState.chips - EPSILON && action.amount <= playerState.chips + EPSILON

            is Action.PostBlind -> action.amount <= playerState.chips + EPSILON

            // Showdown actions are validated by processShowdownAction, not here
            is Action.Show, is Action.Muck, is Action.Collect -> false
        }
    }

    private fun isValidDenomination(amount: ChipAmount): Boolean {
        val remainder = amount % structure.minDenomination
        return remainder < EPSILON || remainder > structure.minDenomination - EPSILON
    }

    companion object {
        private const val EPSILON = 0.000001
    }
}
