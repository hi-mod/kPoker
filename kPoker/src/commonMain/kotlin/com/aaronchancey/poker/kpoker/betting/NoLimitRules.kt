package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState

/**
 * Betting rules for No-Limit games.
 *
 * In no-limit poker, players can bet any amount from the minimum bet up to their entire stack.
 * There's no cap on bet sizing - you can always go all-in.
 */
class NoLimitRules(private val structure: BettingStructure) : BettingRules {

    override fun calculateBetLimits(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): BetLimits = BetLimits(
        minBet = if (currentBet > 0.0) currentBet else structure.minBet,
        maxBet = playerState.chips,
        minRaise = currentBet + minRaise,
    )

    override fun validateBetAmount(
        amount: ChipAmount,
        limits: BetLimits,
        isRaise: Boolean,
    ): Boolean = isAmountInRange(amount, limits)
}
