package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState

/**
 * Betting rules for Fixed-Limit games.
 *
 * In limit poker, bets and raises are fixed amounts - players have no sizing freedom.
 * The bet size is typically the big blind on early streets and 2x big blind on later streets.
 * Players must bet exactly the fixed amount (or go all-in if short-stacked).
 */
class LimitRules(private val structure: BettingStructure) : BettingRules {

    override fun calculateBetLimits(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): BetLimits {
        val fixedBetSize = structure.minBet
        return BetLimits(
            minBet = fixedBetSize,
            maxBet = fixedBetSize, // In limit, min == max (fixed sizing)
            minRaise = fixedBetSize,
        )
    }

    override fun validateBetAmount(
        amount: ChipAmount,
        limits: BetLimits,
        isRaise: Boolean,
    ): Boolean {
        // In limit poker, bets must be exactly the fixed amount
        // The only exception is all-in when short-stacked (handled by caller)
        return kotlin.math.abs(amount - limits.minBet) < CHIP_EPSILON
    }
}
