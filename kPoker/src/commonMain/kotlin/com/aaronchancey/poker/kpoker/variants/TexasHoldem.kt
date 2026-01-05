package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.evaluation.StandardHandEvaluator
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerStatus

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

    override val variantName = TexasHoldemVariant.name
    override val holeCardCount = TexasHoldemVariant.holeCardCount
    override val usesCommunityCards = TexasHoldemVariant.usesCommunityCards

    override fun dealHoleCards() {
        state = state.withPhase(GamePhase.DEALING)

        var table = state.table

        // Deal 2 cards to each active player
        for (seat in state.table.occupiedSeats) {
            if (seat.playerState != null) {
                val cards = state.deck.deal(holeCardCount)
                table = table.updateSeat(seat.number) { s ->
                    s.updatePlayerState { ps ->
                        ps.withHoleCards(cards).withStatus(PlayerStatus.ACTIVE)
                    }
                }
                emit(GameEvent.HoleCardsDealt(seat.playerState.player.id, cards))
            }
        }

        state = state.withTable(table)
    }

    override fun evaluateHands(): List<Winner> {
        val playersInHand = state.table.getPlayersInHand()

        // If only one player remains, they win
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

        // Evaluate each player's best hand
        val playerHands = playersInHand.map { player ->
            val allCards = player.holeCards + state.communityCards
            val bestHand = handEvaluator.findBestHand(allCards, 5)
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
        fun noLimit(smallBlind: ChipAmount, bigBlind: ChipAmount): TexasHoldemGame = TexasHoldemGame(BettingStructure.noLimit(smallBlind, bigBlind))

        fun potLimit(smallBlind: ChipAmount, bigBlind: ChipAmount): TexasHoldemGame = TexasHoldemGame(BettingStructure.potLimit(smallBlind, bigBlind))

        fun fixedLimit(smallBlind: ChipAmount, bigBlind: ChipAmount): TexasHoldemGame = TexasHoldemGame(BettingStructure.fixedLimit(smallBlind, bigBlind))
    }
}
