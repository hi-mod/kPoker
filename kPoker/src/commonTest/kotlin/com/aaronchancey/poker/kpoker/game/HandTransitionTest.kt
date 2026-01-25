package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.ShowdownStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for state transitions between hands.
 * Verifies that player state is properly reset when a new hand begins.
 */
class HandTransitionTest {

    private fun createGame(): TexasHoldemGame = TexasHoldemGame.noLimit(smallBlind = 1.0, bigBlind = 2.0)

    private fun createHeadsUpTable(): Table {
        var table = Table.create("1", "Heads Up Table", 2)
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")

        table = table.sitPlayer(1, PlayerState(alice, chips = 100.0))
        table = table.sitPlayer(2, PlayerState(bob, chips = 100.0))

        return table
    }

    @Test
    fun testShowdownStatusIsResetBetweenHands() {
        val game = createGame()
        val table = createHeadsUpTable()
        game.initialize(table)

        // Play first hand to completion (one player folds pre-flop)
        game.startHand()
        val firstState = game.currentState

        // Get the current actor and have them fold
        val currentActorId = firstState.currentActor!!.player.id
        game.processAction(Action.Fold(currentActorId))

        // Verify hand completed
        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase, "Hand should be complete after fold")

        // Start second hand - this is where the bug manifested
        game.startHand()
        val secondHandState = game.currentState

        // Verify ALL player showdown statuses are reset to null
        for (seat in secondHandState.table.occupiedSeats) {
            val playerState = seat.playerState!!
            assertNull(
                playerState.showdownStatus,
                "showdownStatus should be null at start of new hand for ${playerState.player.name}",
            )
        }
    }

    @Test
    fun testShowdownStatusClearedAfterActualShowdown() {
        val game = createGame()
        val table = createHeadsUpTable()
        game.initialize(table)

        // Play hand 1 to showdown (check/call through all streets)
        game.startHand()
        playToShowdown(game)

        // Both players show
        val showdownState = game.currentState
        assertEquals(GamePhase.SHOWDOWN, showdownState.phase)

        val actor1 = showdownState.currentActor!!
        game.processShowdownAction(Action.Show(actor1.player.id))

        val actor2 = game.currentState.currentActor!!
        game.processShowdownAction(Action.Show(actor2.player.id))

        // Verify hand completed with showdown statuses set
        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase)
        for (seat in game.currentState.table.occupiedSeats) {
            assertEquals(
                ShowdownStatus.SHOWN,
                seat.playerState!!.showdownStatus,
                "Both players should have SHOWN status after showdown",
            )
        }

        // Start next hand
        game.startHand()

        // Verify showdown statuses are cleared
        for (seat in game.currentState.table.occupiedSeats) {
            assertNull(
                seat.playerState!!.showdownStatus,
                "showdownStatus should be null at start of new hand after showdown",
            )
        }
    }

    /**
     * Plays a heads-up hand through all betting rounds to reach showdown.
     * Both players check/call through all streets.
     */
    private fun playToShowdown(game: TexasHoldemGame) {
        // Pre-flop: SB calls, BB checks
        val preFlopActor = game.currentState.currentActor!!
        game.processAction(Action.Call(preFlopActor.player.id, 1.0))
        val bbActor = game.currentState.currentActor!!
        game.processAction(Action.Check(bbActor.player.id))

        // Flop, Turn, River: Both check
        repeat(3) {
            val firstActor = game.currentState.currentActor!!
            game.processAction(Action.Check(firstActor.player.id))
            val secondActor = game.currentState.currentActor!!
            game.processAction(Action.Check(secondActor.player.id))
        }
    }
}
