package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for sit-out logic:
 * - Sitting-out players are excluded from hands (dealing, blinds)
 * - Sit-out is sticky across hand resets
 * - Toggle sit-out transitions between WAITING and SITTING_OUT
 * - Mid-hand sit-out uses sitOutNextHand flag
 * - Hand doesn't start with insufficient non-sitting-out players
 */
class SitOutTest {

    private fun createGame(): TexasHoldemGame = TexasHoldemGame.noLimit(smallBlind = 1.0, bigBlind = 2.0)

    @Test
    fun sittingOutPlayerNotDealtCards() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 3)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(
            2,
            PlayerState(
                Player("p2", "Bob"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )
        table = table.sitPlayer(3, PlayerState(Player("p3", "Charlie"), chips = 100.0))

        game.initialize(table)
        game.startHand()

        val state = game.currentState
        val bobState = state.table.getSeat(2)?.playerState!!

        assertEquals(PlayerStatus.SITTING_OUT, bobState.status, "Bob should still be SITTING_OUT")
        assertTrue(bobState.holeCards.isEmpty(), "Sitting-out Bob should not have hole cards")

        val aliceState = state.table.getSeat(1)?.playerState!!
        assertTrue(aliceState.holeCards.isNotEmpty(), "Alice should have hole cards")
    }

    @Test
    fun sittingOutPlayerExcludedFromBlinds() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 3)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(
            2,
            PlayerState(
                Player("p2", "Bob"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )
        table = table.sitPlayer(3, PlayerState(Player("p3", "Charlie"), chips = 100.0))

        game.initialize(table)
        game.startHand()

        val state = game.currentState
        val bobState = state.table.getSeat(2)?.playerState!!

        assertFalse(bobState.isSmallBlind, "Sitting-out Bob should not be small blind")
        assertFalse(bobState.isBigBlind, "Sitting-out Bob should not be big blind")
        assertEquals(100.0, bobState.chips, "Sitting-out Bob's chips should be unchanged")
    }

    @Test
    fun handDoesNotStartWithOnlyOnNonSittingOutPlayer() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 3)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(
            2,
            PlayerState(
                Player("p2", "Bob"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )
        table = table.sitPlayer(
            3,
            PlayerState(
                Player("p3", "Charlie"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )

        game.initialize(table)

        assertFailsWith<IllegalArgumentException>("Should fail to start hand with only 1 eligible player") {
            game.startHand()
        }
    }

    @Test
    fun sitOutStatusStickyAcrossHandReset() {
        val sittingOut = PlayerState(
            Player("p1", "Alice"),
            chips = 100.0,
            status = PlayerStatus.SITTING_OUT,
        )
        val reset = sittingOut.resetForNewHand()

        assertEquals(
            PlayerStatus.SITTING_OUT,
            reset.status,
            "SITTING_OUT should persist through resetForNewHand()",
        )
    }

    @Test
    fun sitOutNextHandFlagCausesSitOutOnReset() {
        val active = PlayerState(
            Player("p1", "Alice"),
            chips = 100.0,
            status = PlayerStatus.ACTIVE,
            sitOutNextHand = true,
        )
        val reset = active.resetForNewHand()

        assertEquals(
            PlayerStatus.SITTING_OUT,
            reset.status,
            "sitOutNextHand should cause SITTING_OUT on reset",
        )
        assertFalse(reset.sitOutNextHand, "sitOutNextHand flag should be cleared after reset")
    }

    @Test
    fun toggleSitOutFromWaiting() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 2)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(2, PlayerState(Player("p2", "Bob"), chips = 100.0))

        game.initialize(table)

        // Toggle sit-out for Alice (WAITING -> SITTING_OUT)
        val isSittingOut = game.toggleSitOut("p1")
        assertTrue(isSittingOut, "toggleSitOut should return true when sitting out")

        val aliceState = game.currentState.table.getPlayerSeat("p1")?.playerState!!
        assertEquals(PlayerStatus.SITTING_OUT, aliceState.status)
    }

    @Test
    fun toggleSitInFromSittingOut() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 2)

        table = table.sitPlayer(
            1,
            PlayerState(
                Player("p1", "Alice"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )
        table = table.sitPlayer(2, PlayerState(Player("p2", "Bob"), chips = 100.0))

        game.initialize(table)

        // Toggle sit-in for Alice (SITTING_OUT -> WAITING)
        val isSittingOut = game.toggleSitOut("p1")
        assertFalse(isSittingOut, "toggleSitOut should return false when sitting back in")

        val aliceState = game.currentState.table.getPlayerSeat("p1")?.playerState!!
        assertEquals(PlayerStatus.WAITING, aliceState.status)
    }

    @Test
    fun toggleSitOutMidHandSetsSitOutNextHand() {
        val game = createGame()
        var table = Table.create("1", "Test Table", 3)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(2, PlayerState(Player("p2", "Bob"), chips = 100.0))
        table = table.sitPlayer(3, PlayerState(Player("p3", "Charlie"), chips = 100.0))

        game.initialize(table)
        game.startHand()

        // Alice is ACTIVE in the hand - toggling sit-out should set the flag
        val isSittingOut = game.toggleSitOut("p1")
        assertTrue(isSittingOut, "Should return true - will sit out next hand")

        val aliceState = game.currentState.table.getPlayerSeat("p1")?.playerState!!
        assertEquals(PlayerStatus.ACTIVE, aliceState.status, "Should remain ACTIVE during current hand")
        assertTrue(aliceState.sitOutNextHand, "sitOutNextHand should be set")
    }

    @Test
    fun toggleSitOutForUnseatedPlayerThrows() {
        val game = createGame()
        val table = Table.create("1", "Test Table", 2)
        game.initialize(table)

        assertFailsWith<IllegalArgumentException> {
            game.toggleSitOut("nobody")
        }
    }

    @Test
    fun eligiblePlayerCountExcludesSittingOut() {
        var table = Table.create("1", "Test Table", 4)

        table = table.sitPlayer(1, PlayerState(Player("p1", "Alice"), chips = 100.0))
        table = table.sitPlayer(
            2,
            PlayerState(
                Player("p2", "Bob"),
                chips = 100.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )
        table = table.sitPlayer(3, PlayerState(Player("p3", "Charlie"), chips = 50.0))
        table = table.sitPlayer(
            4,
            PlayerState(
                Player("p4", "Diana"),
                chips = 200.0,
                status = PlayerStatus.SITTING_OUT,
            ),
        )

        assertEquals(4, table.playerCount, "Total player count should be 4")
        assertEquals(2, table.eligiblePlayerCount, "Eligible player count should exclude sitting-out")
        assertEquals(
            listOf(1, 3),
            table.seatsWithChips.map { it.number },
            "seatsWithChips should exclude sitting-out players",
        )
    }
}
