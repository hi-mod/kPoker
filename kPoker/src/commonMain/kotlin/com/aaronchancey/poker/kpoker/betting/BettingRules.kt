package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState

interface BettingRules {
    /**
     * Calculates the minimum and maximum bet/raise limits for a player based on game rules.
     */
    fun calculateBetLimits(
        playerState: PlayerState,
        currentBet: ChipAmount,
        potSize: ChipAmount,
        minRaise: ChipAmount,
    ): BetLimits

    /**
     * Validates that a bet/raise amount conforms to the betting rules for this game type.
     *
     * @param amount The bet/raise amount to validate
     * @param limits The calculated bet limits for the current situation
     * @param isRaise True if this is a raise, false if it's an opening bet
     * @return True if the amount is valid according to the betting rules
     */
    fun validateBetAmount(
        amount: ChipAmount,
        limits: BetLimits,
        isRaise: Boolean,
    ): Boolean
}

data class BetLimits(
    val minBet: ChipAmount,
    val maxBet: ChipAmount,
    val minRaise: ChipAmount,
)

/** Tolerance for floating-point comparisons in chip amounts. */
internal const val CHIP_EPSILON = 0.000001

/**
 * Validates that an amount falls within the given bet limits (inclusive, with epsilon tolerance).
 */
internal fun isAmountInRange(amount: ChipAmount, limits: BetLimits): Boolean = amount >= limits.minBet - CHIP_EPSILON && amount <= limits.maxBet + CHIP_EPSILON
