package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.evaluation.StandardHandEvaluator
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.ShowdownStatus

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
) : PokerGame(bettingStructure, StandardHandEvaluator()) {

    override val gameVariant = GameVariant.TEXAS_HOLDEM
    override val variantName = TexasHoldemVariant.name
    override val holeCardCount = TexasHoldemVariant.holeCardCount
    override val usesCommunityCards = TexasHoldemVariant.usesCommunityCards

    override fun evaluateHands(): List<Winner> {
        val playersInHand = state.table.getPlayersInHand()

        // If only one player remains, they win (no showdown needed)
        if (playersInHand.size == 1) {
            val winner = playersInHand.first()
            return listOf(
                Winner(
                    playerId = winner.player.id,
                    amount = state.totalPot,
                    handDescription = "Last player standing",
                ),
            )
        }

        // Only players who SHOWED their cards are eligible for pot
        val showingPlayers = playersInHand.filter { it.showdownStatus == ShowdownStatus.SHOWN }

        // If everyone mucked (shouldn't happen if rules enforced), give to first in hand
        if (showingPlayers.isEmpty()) {
            return listOf(
                Winner(
                    playerId = playersInHand.first().player.id,
                    amount = state.totalPot,
                    handDescription = "No shown hands",
                ),
            )
        }

        // Evaluate each showing player's best hand
        val playerHands = showingPlayers.map { player ->
            val allCards = player.holeCards + state.communityCards
            // findBestHand now returns a List, take the first/only one
            val bestHand = handEvaluator.findBestHand(allCards, 5).first()
            player to bestHand
        }

        // Find the best hand(s)
        val sortedHands = playerHands.sortedByDescending { it.second }
        val bestHand = sortedHands.first().second

        // Find all players with the best hand (for splits)
        val winners = sortedHands.filter { it.second.compareTo(bestHand) == 0 }

        // Calculate winnings from each pot
        val result = mutableListOf<Winner>()
        for (pot in state.potManager.pots) {
            val eligibleWinners = winners.filter { it.first.player.id in pot.eligiblePlayerIds }
            if (eligibleWinners.isNotEmpty()) {
                val share = pot.amount / eligibleWinners.size
                for ((player, hand) in eligibleWinners) {
                    result.add(
                        Winner(
                            playerId = player.player.id,
                            amount = share,
                            handDescription = hand.description(),
                            potType = if (pot.isMain) "main" else "side",
                        ),
                    )
                }
            }
        }

        return result
    }

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
