package com.aaronchancey.poker.kpoker.equity

import com.aaronchancey.poker.kpoker.player.ChipAmount

/**
 * Expected value results for available actions.
 *
 * @param equity Hero's showdown win probability (0.0 to 1.0)
 * @param foldEv Always 0 — folding is the baseline reference
 * @param checkEv EV of checking (null if check is not available)
 * @param callEv EV of calling (null if call is not available)
 */
data class ActionEv(
    val equity: Double,
    val foldEv: ChipAmount = 0.0,
    val checkEv: ChipAmount? = null,
    val callEv: ChipAmount? = null,
)

/**
 * Computes expected values for fold/check/call actions given equity and pot geometry.
 *
 * Uses simplified pot-odds EV formulas that assume no future betting streets.
 * Bet/raise EVs are intentionally excluded because they require fold equity
 * assumptions we don't have (we'd need opponent range models).
 */
object ActionEvCalculator {

    /**
     * Calculates [ActionEv] for the available actions.
     *
     * @param equity Hero's showdown equity (0.0 to 1.0)
     * @param potSize Current effective pot (collected pots + uncollected bets)
     * @param amountToCall Amount hero must put in to call (0.0 if no bet to face)
     * @param canCheck Whether CHECK is a valid action
     * @param canCall Whether CALL is a valid action
     * @return [ActionEv] with computed values for available actions
     */
    fun calculate(
        equity: Double,
        potSize: ChipAmount,
        amountToCall: ChipAmount,
        canCheck: Boolean,
        canCall: Boolean,
    ): ActionEv {
        val checkEv = if (canCheck) equity * potSize else null
        val callEv = if (canCall) equity * (potSize + amountToCall) - amountToCall else null

        return ActionEv(
            equity = equity,
            checkEv = checkEv,
            callEv = callEv,
        )
    }
}
