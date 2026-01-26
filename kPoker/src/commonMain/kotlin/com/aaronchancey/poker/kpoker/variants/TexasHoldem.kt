package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.variants.PokerVariant
import com.aaronchancey.poker.kpoker.variants.TexasHoldemStrategy

object TexasHoldemVariant : PokerVariant {
    override val name = "Texas Hold'em"
    override val holeCardCount = 2
    override val usesCommunityCards = true
    override val minPlayers = 2
    override val maxPlayers = 10
    override val description = "Each player receives 2 hole cards. " +
        "5 community cards are dealt. Best 5-card hand wins using any combination."
}

class TexasHoldemGame(
    bettingStructure: BettingStructure,
) : PokerGame(bettingStructure, TexasHoldemStrategy()) {

    companion object {
        fun noLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): TexasHoldemGame = TexasHoldemGame(
            BettingStructure.noLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
        )

        fun potLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): TexasHoldemGame = TexasHoldemGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
        )

        fun fixedLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): TexasHoldemGame = TexasHoldemGame(
            BettingStructure.fixedLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
        )
    }
}
