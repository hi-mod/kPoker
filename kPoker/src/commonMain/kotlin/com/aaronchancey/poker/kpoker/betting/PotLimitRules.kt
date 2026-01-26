package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState

/**
 * Betting rules for Pot-Limit games.
 *
 * In pot-limit poker, the maximum bet is the size of the pot (including the call amount).
 * The pot-limit formula for a raise is: pot + all bets + call amount.
 */
class PotLimitRules(private val structure: BettingStructure) : BettingRules {

    override fun calculateBetLimits(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): BetLimits = BetLimits(
        minBet = if (currentBet > 0.0) currentBet else structure.minBet,
        maxBet = minOf(playerState.chips, potSize + (currentBet * 2) - playerState.currentBet),
        minRaise = currentBet + minRaise,
    )

    override fun validateBetAmount(
        amount: ChipAmount,
        limits: BetLimits,
        isRaise: Boolean,
    ): Boolean = isAmountInRange(amount, limits)
}
