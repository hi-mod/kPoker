package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.rake.RakeCalculator

object OmahaVariant : PokerVariant {
    override val name = "Omaha"
    override val holeCardCount = 4
    override val usesCommunityCards = true
    override val minPlayers = 2
    override val maxPlayers = 10
    override val description = "Each player receives 4 hole cards. " +
        "Must use exactly 2 hole cards and 3 community cards."
}

object OmahaHiLoVariant : PokerVariant {
    override val name = "Omaha Hi-Lo"
    override val holeCardCount = 4
    override val usesCommunityCards = true
    override val minPlayers = 2
    override val maxPlayers = 10
    override val description = "Omaha split pot variant. " +
        "Best high hand splits with best qualifying low hand (8 or better)."
}

class OmahaGame(
    bettingStructure: BettingStructure,
    private val isHiLo: Boolean = false,
    rakeCalculator: RakeCalculator? = null,
) : PokerGame(bettingStructure, OmahaStrategy(isHiLo), rakeCalculator = rakeCalculator) {

    companion object {
        fun potLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            minDenomination: ChipAmount = 1.0,
            rakeCalculator: RakeCalculator? = null,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                ante = ante,
                minDenomination = minDenomination,
            ),
            rakeCalculator = rakeCalculator,
        )

        fun potLimitHiLo(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            minDenomination: ChipAmount = 1.0,
            rakeCalculator: RakeCalculator? = null,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                ante = ante,
                minDenomination = minDenomination,
            ),
            isHiLo = true,
            rakeCalculator = rakeCalculator,
        )
    }
}
