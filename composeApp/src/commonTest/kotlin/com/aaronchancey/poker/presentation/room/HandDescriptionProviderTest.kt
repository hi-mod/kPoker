package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Seat
import com.aaronchancey.poker.kpoker.player.Table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandDescriptionProviderTest {
    private val provider = HandDescriptionProvider()
    private val testPlayerId = "player-1"

    private fun cards(vararg cardStrings: String): List<Card> = cardStrings.map { Card.fromString(it) }

    private fun createGameState(
        playerId: String = testPlayerId,
        holeCards: List<Card> = emptyList(),
        communityCards: List<Card> = emptyList(),
        variant: GameVariant = GameVariant.TEXAS_HOLDEM,
    ): GameState {
        val player = Player(id = playerId, name = "Test Player")
        val playerState = PlayerState(
            player = player,
            chips = 1000.0,
            holeCards = holeCards,
            status = PlayerStatus.ACTIVE,
        )
        val seat = Seat(number = 1, playerState = playerState)
        val table = Table(
            id = "test-table",
            name = "Test Table",
            seats = listOf(seat) + (2..6).map { Seat(number = it) },
        )
        return GameState(
            table = table,
            variant = variant,
            communityCards = communityCards,
        )
    }

    // Null/empty cases

    @Test
    fun testNullGameState_returnsEmpty() {
        val result = provider.getHandDescription(null, testPlayerId)
        assertEquals("", result)
    }

    @Test
    fun testNullPlayerId_returnsEmpty() {
        val gameState = createGameState()
        val result = provider.getHandDescription(gameState, null)
        assertEquals("", result)
    }

    @Test
    fun testPlayerNotInGame_returnsEmpty() {
        val gameState = createGameState(holeCards = cards("As", "Kh"))
        val result = provider.getHandDescription(gameState, "unknown-player")
        assertEquals("", result)
    }

    @Test
    fun testNoHoleCards_returnsEmpty() {
        val gameState = createGameState(holeCards = emptyList())
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertEquals("", result)
    }

    // Pre-flop (partial hand evaluation)

    @Test
    fun testPreFlop_pocketPair() {
        val gameState = createGameState(
            holeCards = cards("Ah", "As"),
            communityCards = emptyList(),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("One Pair"), "Expected 'One Pair' description, got: $result")
    }

    @Test
    fun testPreFlop_highCard() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kd"),
            communityCards = emptyList(),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("High Card"), "Expected 'High Card' description, got: $result")
    }

    @Test
    fun testPreFlop_twoCommunityCards_stillPartial() {
        val gameState = createGameState(
            holeCards = cards("Ah", "As"),
            communityCards = cards("Kd", "Qc"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("One Pair"), "With <3 community cards should use partial evaluation, got: $result")
    }

    // Post-flop (full hand evaluation)

    @Test
    fun testFlop_pair() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kd"),
            communityCards = cards("As", "9c", "5h"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("One Pair"), "Expected 'One Pair' description, got: $result")
    }

    @Test
    fun testFlop_twoPair() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kd"),
            communityCards = cards("As", "Kc", "5h"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Two Pair"), "Expected 'Two Pair' description, got: $result")
    }

    @Test
    fun testFlop_threeOfAKind() {
        val gameState = createGameState(
            holeCards = cards("Ah", "As"),
            communityCards = cards("Ad", "Kc", "5h"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Three of a Kind"), "Expected 'Three of a Kind' description, got: $result")
    }

    @Test
    fun testRiver_straight() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kd"),
            communityCards = cards("Qs", "Jc", "Th", "2d", "3s"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Straight"), "Expected 'Straight' description, got: $result")
    }

    @Test
    fun testRiver_flush() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kh"),
            communityCards = cards("Qh", "9h", "5h", "2d", "3s"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Flush"), "Expected 'Flush' description, got: $result")
    }

    @Test
    fun testRiver_fullHouse() {
        val gameState = createGameState(
            holeCards = cards("Ah", "As"),
            communityCards = cards("Ad", "Kc", "Kh", "2d", "3s"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Full House"), "Expected 'Full House' description, got: $result")
    }

    @Test
    fun testRiver_royalFlush() {
        val gameState = createGameState(
            holeCards = cards("Ah", "Kh"),
            communityCards = cards("Qh", "Jh", "Th", "2d", "3s"),
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        assertTrue(result.contains("Royal Flush"), "Expected 'Royal Flush' description, got: $result")
    }

    // Variant tests

    @Test
    fun testOmaha_usesCorrectEvaluator() {
        // In Omaha, must use exactly 2 hole cards + 3 community cards
        val gameState = createGameState(
            holeCards = cards("Ah", "Kh", "Qd", "Jc"),
            communityCards = cards("Th", "9h", "8h", "2d", "3s"),
            variant = GameVariant.OMAHA,
        )
        val result = provider.getHandDescription(gameState, testPlayerId)
        // Should have a valid hand description (flush using Ah, Kh from hole + 3 hearts from community)
        assertTrue(result.isNotEmpty(), "Expected hand description for Omaha, got empty")
    }
}
