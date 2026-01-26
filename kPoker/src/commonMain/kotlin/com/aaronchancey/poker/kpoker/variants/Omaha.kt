package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.variants.OmahaStrategy
import com.aaronchancey.poker.kpoker.variants.PokerVariant

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
) : PokerGame(bettingStructure, OmahaStrategy(isHiLo)) {

    companion object {
        fun potLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
        )

        fun potLimitHiLo(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
            isHiLo = true,
        )
    }
}
