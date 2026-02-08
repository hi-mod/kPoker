package com.aaronchancey.poker.kpoker.rake

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Pot
import kotlin.math.floor

/**
 * Calculates rake amounts for each pot independently.
 *
 * Returns a list of rake amounts in pot order — one entry per pot.
 * Each pot is raked and capped independently.
 */
fun interface RakeCalculator {
    fun calculateRake(pots: List<Pot>): List<ChipAmount>
}

/**
 * Standard percentage-based rake with per-pot cap and denomination rounding.
 *
 * @param rakePercent The percentage of each pot to rake (e.g., 0.05 for 5%)
 * @param rakeCap Maximum rake per individual pot
 * @param minDenomination Rake is floored to the nearest multiple of this value
 */
class PercentageRakeCalculator(
    private val rakePercent: Double,
    private val rakeCap: ChipAmount,
    private val minDenomination: ChipAmount,
) : RakeCalculator {

    override fun calculateRake(pots: List<Pot>): List<ChipAmount> = pots.map { pot ->
        val raw = (pot.amount * rakePercent).coerceIn(0.0..rakeCap)
        floor(raw / minDenomination) * minDenomination
    }
}
