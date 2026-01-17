package com.aaronchancey.poker.kpoker.dealing

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.core.DealtCard
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerStatus

/**
 * Standard card dealer implementation for traditional online poker.
 *
 * In this model:
 * - Server has full knowledge of the deck
 * - Cards are dealt directly from a shuffled deck
 * - Visibility filtering is handled separately by [VisibilityService]
 *
 * This is the trust model used by most online poker sites where
 * players trust the server to deal fairly.
 */
class StandardDealer : CardDealer {

    override fun dealHoleCards(
        state: GameState,
        cardsPerPlayer: Int,
        visibilityPattern: List<CardVisibility>?,
    ): DealResult.HoleCards {
        var currentState = state
        val dealtCards = mutableMapOf<PlayerId, List<Card>>()

        // Default to all cards being private (face-down) if no pattern specified
        val pattern = visibilityPattern ?: List(cardsPerPlayer) { CardVisibility.PRIVATE }
        require(pattern.size == cardsPerPlayer) {
            "Visibility pattern size (${pattern.size}) must match cardsPerPlayer ($cardsPerPlayer)"
        }

        // Deal to each occupied seat in seat order
        for (seat in currentState.table.occupiedSeats.sortedBy { it.number }) {
            val playerState = seat.playerState ?: continue

            // Deal cards from deck
            val cards = currentState.deck.deal(cardsPerPlayer)
            dealtCards[playerState.player.id] = cards

            // Create DealtCards with visibility pattern applied
            val dealt = cards.mapIndexed { index, card ->
                DealtCard(card = card, visibility = pattern[index])
            }

            // Update player state with dealt cards (which syncs holeCards) and mark as active
            currentState = currentState.withTable(
                currentState.table.updateSeat(seat.number) { s ->
                    s.updatePlayerState { ps ->
                        ps.withDealtCards(dealt).withStatus(PlayerStatus.ACTIVE)
                    }
                },
            )
        }

        return DealResult.HoleCards(
            updatedState = currentState,
            dealtCards = dealtCards,
        )
    }

    override fun dealCommunityCards(
        state: GameState,
        count: Int,
        burnFirst: Boolean,
    ): DealResult.CommunityCards {
        // Burn a card if required (standard poker rule)
        if (burnFirst) {
            state.deck.burn()
        }

        // Deal community cards
        val cards = state.deck.deal(count)
        val updatedState = state.addCommunityCards(cards)

        return DealResult.CommunityCards(
            updatedState = updatedState,
            cards = cards,
        )
    }
}
