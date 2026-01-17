package com.aaronchancey.poker.kpoker.dealing

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId

/**
 * Abstraction for card dealing operations.
 *
 * This interface enables different dealing implementations:
 * - [StandardDealer]: Traditional online poker where server deals from shuffled deck
 * - Future: Mental poker protocols with cryptographic multi-party shuffle
 *
 * The abstraction separates "what cards to deal" from "how dealing works",
 * allowing the same game logic to run with different trust models.
 */
interface CardDealer {
    /**
     * Deal hole cards to all active players at the table.
     *
     * @param state Current game state containing the table and deck
     * @param cardsPerPlayer Number of hole cards per player (2 for Hold'em, 4 for Omaha)
     * @param visibilityPattern Optional pattern specifying visibility for each card position.
     *        For example, `[PRIVATE, PRIVATE, PUBLIC]` for Stud where third card is face-up.
     *        If null, all cards are dealt face-down (PRIVATE).
     * @return Result containing updated state and map of dealt cards per player
     */
    fun dealHoleCards(
        state: GameState,
        cardsPerPlayer: Int,
        visibilityPattern: List<CardVisibility>? = null,
    ): DealResult.HoleCards

    /**
     * Deal community cards (flop, turn, river).
     *
     * @param state Current game state
     * @param count Number of cards to deal (3 for flop, 1 for turn/river)
     * @param burnFirst Whether to burn a card before dealing (standard poker rule)
     * @return Result containing updated state and the dealt cards
     */
    fun dealCommunityCards(state: GameState, count: Int, burnFirst: Boolean = true): DealResult.CommunityCards
}

/**
 * Results from dealing operations.
 */
sealed class DealResult {
    /**
     * Result of dealing hole cards to players.
     *
     * @property updatedState Game state with cards dealt to player states
     * @property dealtCards Map of player ID to their dealt hole cards
     */
    data class HoleCards(
        val updatedState: GameState,
        val dealtCards: Map<PlayerId, List<Card>>,
    ) : DealResult()

    /**
     * Result of dealing community cards.
     *
     * @property updatedState Game state with new community cards added
     * @property cards The cards that were dealt
     */
    data class CommunityCards(
        val updatedState: GameState,
        val cards: List<Card>,
    ) : DealResult()
}
