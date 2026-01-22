package com.aaronchancey.poker.kpoker.visibility

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Rank
import com.aaronchancey.poker.kpoker.core.Suit
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.ShowdownStatus
import com.aaronchancey.poker.kpoker.player.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardVisibilityTest {

    private val visibility = StandardVisibility()

    private fun createCard(rank: Rank, suit: Suit) = Card(rank, suit)

    private fun createStateWithPlayers(): GameState {
        var table = Table.create("test", "Test Table", 9)

        // Player 1 with hole cards
        val p1 = Player("p1", "Alice")
        val p1Cards = listOf(createCard(Rank.ACE, Suit.SPADES), createCard(Rank.KING, Suit.SPADES))
        table = table.sitPlayer(1, PlayerState(p1, chips = 100.0, holeCards = p1Cards, status = PlayerStatus.ACTIVE))

        // Player 2 with hole cards
        val p2 = Player("p2", "Bob")
        val p2Cards = listOf(createCard(Rank.QUEEN, Suit.HEARTS), createCard(Rank.JACK, Suit.HEARTS))
        table = table.sitPlayer(2, PlayerState(p2, chips = 100.0, holeCards = p2Cards, status = PlayerStatus.ACTIVE))

        // Player 3 with hole cards
        val p3 = Player("p3", "Charlie")
        val p3Cards = listOf(createCard(Rank.TEN, Suit.DIAMONDS), createCard(Rank.NINE, Suit.DIAMONDS))
        table = table.sitPlayer(3, PlayerState(p3, chips = 100.0, holeCards = p3Cards, status = PlayerStatus.ACTIVE))

        return GameState(table = table, phase = GamePhase.FLOP)
    }

    private fun createStateWithPlayersShown(): GameState {
        var table = Table.create("test", "Test Table", 9)

        // Player 1 with SHOWN status
        val p1 = Player("p1", "Alice")
        val p1Cards = listOf(createCard(Rank.ACE, Suit.SPADES), createCard(Rank.KING, Suit.SPADES))
        table = table.sitPlayer(1, PlayerState(p1, chips = 100.0, holeCards = p1Cards, status = PlayerStatus.ACTIVE, showdownStatus = ShowdownStatus.SHOWN))

        // Player 2 with SHOWN status
        val p2 = Player("p2", "Bob")
        val p2Cards = listOf(createCard(Rank.QUEEN, Suit.HEARTS), createCard(Rank.JACK, Suit.HEARTS))
        table = table.sitPlayer(2, PlayerState(p2, chips = 100.0, holeCards = p2Cards, status = PlayerStatus.ACTIVE, showdownStatus = ShowdownStatus.SHOWN))

        // Player 3 with SHOWN status
        val p3 = Player("p3", "Charlie")
        val p3Cards = listOf(createCard(Rank.TEN, Suit.DIAMONDS), createCard(Rank.NINE, Suit.DIAMONDS))
        table = table.sitPlayer(3, PlayerState(p3, chips = 100.0, holeCards = p3Cards, status = PlayerStatus.ACTIVE, showdownStatus = ShowdownStatus.SHOWN))

        return GameState(table = table, phase = GamePhase.SHOWDOWN)
    }

    @Test
    fun getVisibleState_playerSeesOwnCards() {
        val state = createStateWithPlayers()
        val visibleState = visibility.getVisibleState(state, "p1")

        val p1State = visibleState.table.getSeat(1)?.playerState!!
        assertEquals(2, p1State.holeCards.size, "Player should see their own hole cards")
        assertEquals(Rank.ACE, p1State.holeCards[0].rank)
        assertEquals(Rank.KING, p1State.holeCards[1].rank)
    }

    @Test
    fun getVisibleState_playerCannotSeeOtherCards() {
        val state = createStateWithPlayers()
        val visibleState = visibility.getVisibleState(state, "p1")

        // Player 1 should not see Player 2's or Player 3's cards
        val p2State = visibleState.table.getSeat(2)?.playerState!!
        val p3State = visibleState.table.getSeat(3)?.playerState!!

        assertTrue(p2State.holeCards.isEmpty(), "Should not see opponent's cards")
        assertTrue(p3State.holeCards.isEmpty(), "Should not see opponent's cards")
    }

    @Test
    fun getVisibleState_allCardsVisibleAtShowdown() {
        // Players must have SHOWN status for cards to be visible at showdown
        val state = createStateWithPlayersShown().copy(phase = GamePhase.SHOWDOWN)
        val visibleState = visibility.getVisibleState(state, "p1")

        // At showdown, cards visible for players who chose to SHOW
        visibleState.table.occupiedSeats.forEach { seat ->
            val ps = seat.playerState!!
            assertEquals(2, ps.holeCards.size, "Cards visible at showdown for shown players at seat ${seat.number}")
        }
    }

    @Test
    fun getVisibleState_muckedCardsHiddenAtShowdown() {
        // Test that MUCKED players' cards remain hidden
        var table = Table.create("test", "Test Table", 9)

        val p1 = Player("p1", "Alice")
        val p1Cards = listOf(createCard(Rank.ACE, Suit.SPADES), createCard(Rank.KING, Suit.SPADES))
        table = table.sitPlayer(1, PlayerState(p1, chips = 100.0, holeCards = p1Cards, status = PlayerStatus.ACTIVE, showdownStatus = ShowdownStatus.SHOWN))

        val p2 = Player("p2", "Bob")
        val p2Cards = listOf(createCard(Rank.QUEEN, Suit.HEARTS), createCard(Rank.JACK, Suit.HEARTS))
        table = table.sitPlayer(2, PlayerState(p2, chips = 100.0, holeCards = p2Cards, status = PlayerStatus.ACTIVE, showdownStatus = ShowdownStatus.MUCKED))

        val state = GameState(table = table, phase = GamePhase.SHOWDOWN)
        val visibleState = visibility.getVisibleState(state, "p1")

        // P1 showed - their cards visible
        val p1State = visibleState.table.getSeat(1)?.playerState!!
        assertEquals(2, p1State.holeCards.size, "Shown player's cards visible")

        // P2 mucked - their cards hidden
        val p2State = visibleState.table.getSeat(2)?.playerState!!
        assertTrue(p2State.holeCards.isEmpty(), "Mucked player's cards hidden")
    }

    @Test
    fun getVisibleState_emptySeatsUnchanged() {
        val state = createStateWithPlayers()
        val visibleState = visibility.getVisibleState(state, "p1")

        // Empty seats should remain empty
        val emptySeat = visibleState.table.getSeat(4)
        assertTrue(emptySeat?.isEmpty == true)
    }

    @Test
    fun getVisibleState_preservesOtherPlayerInfo() {
        val state = createStateWithPlayers()
        val visibleState = visibility.getVisibleState(state, "p1")

        // Other player info (chips, status) should still be visible
        val p2State = visibleState.table.getSeat(2)?.playerState!!
        assertEquals(100.0, p2State.chips, "Chips should be visible")
        assertEquals(PlayerStatus.ACTIVE, p2State.status, "Status should be visible")
        assertEquals("Bob", p2State.player.name, "Name should be visible")
    }

    @Test
    fun getSpectatorView_hidesAllCardsBeforeShowdown() {
        val state = createStateWithPlayers()
        val spectatorView = visibility.getSpectatorView(state)

        // Spectator should not see any hole cards
        spectatorView.table.occupiedSeats.forEach { seat ->
            val ps = seat.playerState!!
            assertTrue(ps.holeCards.isEmpty(), "Spectator should not see any hole cards")
        }
    }

    @Test
    fun getSpectatorView_showsShownCardsAtShowdown() {
        // Players must have SHOWN status for cards to be visible
        val state = createStateWithPlayersShown()
        val spectatorView = visibility.getSpectatorView(state)

        // At showdown, spectator sees cards for players who SHOWED
        spectatorView.table.occupiedSeats.forEach { seat ->
            val ps = seat.playerState!!
            assertEquals(2, ps.holeCards.size, "Spectator sees shown cards at showdown")
        }
    }

    @Test
    fun getSpectatorView_preservesPlayerInfo() {
        val state = createStateWithPlayers()
        val spectatorView = visibility.getSpectatorView(state)

        // Player info should still be visible to spectators
        val p1State = spectatorView.table.getSeat(1)?.playerState!!
        assertEquals(100.0, p1State.chips)
        assertEquals("Alice", p1State.player.name)
        assertEquals(PlayerStatus.ACTIVE, p1State.status)
    }

    @Test
    fun getVisibleState_worksForNonSeatedPlayer() {
        val state = createStateWithPlayers()
        // "spectator1" is not seated
        val visibleState = visibility.getVisibleState(state, "spectator1")

        // Should hide all cards (like spectator view, but using getVisibleState)
        visibleState.table.occupiedSeats.forEach { seat ->
            val ps = seat.playerState!!
            assertTrue(ps.holeCards.isEmpty(), "Non-seated player should not see any cards")
        }
    }

    @Test
    fun getVisibleState_communityCardsAlwaysVisible() {
        val communityCards = listOf(
            createCard(Rank.TWO, Suit.CLUBS),
            createCard(Rank.THREE, Suit.CLUBS),
            createCard(Rank.FOUR, Suit.CLUBS),
        )
        val state = createStateWithPlayers().copy(communityCards = communityCards)

        val visibleState = visibility.getVisibleState(state, "p1")
        assertEquals(3, visibleState.communityCards.size, "Community cards always visible")

        val spectatorView = visibility.getSpectatorView(state)
        assertEquals(3, spectatorView.communityCards.size, "Community cards visible to spectators")
    }

    @Test
    fun getVisibleState_differentPhasesBeforeShowdown() {
        // Cards should be hidden in all phases before showdown
        val phases = listOf(
            GamePhase.PRE_FLOP,
            GamePhase.FLOP,
            GamePhase.TURN,
            GamePhase.RIVER,
        )

        phases.forEach { phase ->
            val state = createStateWithPlayers().copy(phase = phase)
            val visibleState = visibility.getVisibleState(state, "p1")

            val p2Cards = visibleState.table.getSeat(2)?.playerState?.holeCards
            assertTrue(p2Cards?.isEmpty() == true, "Opponent cards hidden in phase $phase")
        }
    }
}
