package com.aaronchancey.poker.kpoker.dealing

import com.aaronchancey.poker.kpoker.core.Deck
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StandardDealerTest {

    private val dealer = StandardDealer()

    private fun createTableWithPlayers(playerCount: Int): Table {
        var table = Table.create("test", "Test Table", 9)
        repeat(playerCount) { i ->
            val player = Player("p${i + 1}", "Player ${i + 1}")
            table = table.sitPlayer(i + 1, PlayerState(player, chips = 100.0))
        }
        return table
    }

    private fun createGameState(playerCount: Int): GameState {
        val table = createTableWithPlayers(playerCount)
        return GameState(table = table, deck = Deck.standard())
    }

    @Test
    fun dealHoleCards_dealsCorrectNumberOfCards() {
        val state = createGameState(3)
        val result = dealer.dealHoleCards(state, cardsPerPlayer = 2)

        // Each player should have 2 cards
        assertEquals(3, result.dealtCards.size)
        result.dealtCards.forEach { (_, cards) ->
            assertEquals(2, cards.size)
        }
    }

    @Test
    fun dealHoleCards_dealsUniqueCards() {
        val state = createGameState(3)
        val result = dealer.dealHoleCards(state, cardsPerPlayer = 2)

        // All dealt cards should be unique
        val allCards = result.dealtCards.values.flatten()
        assertEquals(6, allCards.size)
        assertEquals(6, allCards.toSet().size, "All cards should be unique")
    }

    @Test
    fun dealHoleCards_updatesPlayerStates() {
        val state = createGameState(2)
        val result = dealer.dealHoleCards(state, cardsPerPlayer = 2)

        // Players in updated state should have hole cards and be ACTIVE
        result.updatedState.table.occupiedSeats.forEach { seat ->
            val ps = seat.playerState!!
            assertEquals(2, ps.holeCards.size, "Player should have 2 hole cards")
            assertEquals(PlayerStatus.ACTIVE, ps.status, "Player should be ACTIVE after dealing")
        }
    }

    @Test
    fun dealHoleCards_fourCardsForOmaha() {
        val state = createGameState(2)
        val result = dealer.dealHoleCards(state, cardsPerPlayer = 4)

        result.dealtCards.forEach { (_, cards) ->
            assertEquals(4, cards.size, "Omaha players should have 4 cards")
        }

        // 2 players * 4 cards = 8 unique cards
        val allCards = result.dealtCards.values.flatten()
        assertEquals(8, allCards.toSet().size)
    }

    @Test
    fun dealHoleCards_decreasesDeckSize() {
        val state = createGameState(3)
        val initialRemaining = state.deck.remaining

        dealer.dealHoleCards(state, cardsPerPlayer = 2)

        // Deck is mutated (6 cards dealt)
        assertEquals(initialRemaining - 6, state.deck.remaining)
    }

    @Test
    fun dealCommunityCards_dealsFlopCorrectly() {
        val state = createGameState(2)
        val result = dealer.dealCommunityCards(state, count = 3, burnFirst = true)

        assertEquals(3, result.cards.size, "Flop should be 3 cards")
        assertEquals(3, result.updatedState.communityCards.size)
    }

    @Test
    fun dealCommunityCards_dealsTurnCorrectly() {
        // Start with a flop already dealt
        var state = createGameState(2)
        val flopResult = dealer.dealCommunityCards(state, count = 3, burnFirst = true)
        state = flopResult.updatedState

        val turnResult = dealer.dealCommunityCards(state, count = 1, burnFirst = true)

        assertEquals(1, turnResult.cards.size, "Turn should be 1 card")
        assertEquals(4, turnResult.updatedState.communityCards.size, "Should have 4 community cards after turn")
    }

    @Test
    fun dealCommunityCards_dealsRiverCorrectly() {
        // Start with flop and turn already dealt
        var state = createGameState(2)
        state = dealer.dealCommunityCards(state, count = 3, burnFirst = true).updatedState
        state = dealer.dealCommunityCards(state, count = 1, burnFirst = true).updatedState

        val riverResult = dealer.dealCommunityCards(state, count = 1, burnFirst = true)

        assertEquals(1, riverResult.cards.size, "River should be 1 card")
        assertEquals(5, riverResult.updatedState.communityCards.size, "Should have 5 community cards after river")
    }

    @Test
    fun dealCommunityCards_burnCardIsNotDealt() {
        val state = createGameState(2)
        val initialRemaining = state.deck.remaining

        dealer.dealCommunityCards(state, count = 3, burnFirst = true)

        // 1 burn + 3 dealt = 4 cards removed
        assertEquals(initialRemaining - 4, state.deck.remaining)
    }

    @Test
    fun dealCommunityCards_noBurnWhenFalse() {
        val state = createGameState(2)
        val initialRemaining = state.deck.remaining

        dealer.dealCommunityCards(state, count = 3, burnFirst = false)

        // Only 3 dealt, no burn
        assertEquals(initialRemaining - 3, state.deck.remaining)
    }

    @Test
    fun dealCommunityCards_cardsAreUnique() {
        var state = createGameState(2)

        // Deal all community cards
        val flopResult = dealer.dealCommunityCards(state, count = 3, burnFirst = true)
        state = flopResult.updatedState
        val turnResult = dealer.dealCommunityCards(state, count = 1, burnFirst = true)
        state = turnResult.updatedState
        val riverResult = dealer.dealCommunityCards(state, count = 1, burnFirst = true)

        val allCommunity = riverResult.updatedState.communityCards
        assertEquals(5, allCommunity.size)
        assertEquals(5, allCommunity.toSet().size, "All community cards should be unique")
    }
}
