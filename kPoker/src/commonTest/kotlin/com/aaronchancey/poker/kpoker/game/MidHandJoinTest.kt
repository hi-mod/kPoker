package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MidHandJoinTest {

    private fun createGame(): TexasHoldemGame = TexasHoldemGame.noLimit(smallBlind = 10, bigBlind = 20)

    private fun createTableWithTwoPlayers(): Table {
        var table = Table.create("1", "Test Table", 6)
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")

        table = table.sitPlayer(1, PlayerState(alice, chips = 1000))
        table = table.sitPlayer(2, PlayerState(bob, chips = 1000))

        return table
    }

    @Test
    fun testPlayerJoiningMidHandWaits() {
        val game = createGame()
        var table = createTableWithTwoPlayers()
        game.initialize(table)

        // 1. Start the hand with 2 players
        game.startHand()

        var state = game.currentState
        assertTrue(state.isHandInProgress)
        assertEquals(GamePhase.PRE_FLOP, state.phase)
        assertEquals(2, state.playersInHand.size)

        // Verify active players have cards
        val aliceSeat = state.table.getSeat(1)!!
        val bobSeat = state.table.getSeat(2)!!
        assertTrue(aliceSeat.playerState!!.holeCards.isNotEmpty())
        assertTrue(bobSeat.playerState!!.holeCards.isNotEmpty())

        // 2. Charlie joins mid-hand
        val charlie = Player("p3", "Charlie")
        val charlieState = PlayerState(charlie, chips = 1000)

        // Use the table from the current state to ensure we have the latest chips/bets
        table = state.table.sitPlayer(3, charlieState)

        // Update the game table - THIS IS THE KEY API CALL
        game.updateTable(table)

        // 3. Verify Charlie's status
        state = game.currentState
        val charlieSeat = state.table.getSeat(3)!!

        assertEquals(PlayerStatus.WAITING, charlieSeat.playerState!!.status, "Charlie should be WAITING")
        assertTrue(charlieSeat.playerState!!.holeCards.isEmpty(), "Charlie should not have cards")
        assertEquals(3, state.table.playerCount, "Table should have 3 players")
        assertEquals(2, state.playersInHand.size, "Still only 2 players in the hand")

        // 4. Play out the hand (Alice folds to end it quickly)
        val actor = state.currentActor!!
        game.processAction(Action.Fold(actor.player.id))

        state = game.currentState
        assertEquals(GamePhase.HAND_COMPLETE, state.phase)

        // 5. Start next hand
        game.startHand()
        state = game.currentState

        // 6. Verify Charlie is now in the hand
        assertEquals(3, state.playersInHand.size, "All 3 players should be in the new hand")

        val charlieSeatNextHand = state.table.getSeat(3)!!
        assertEquals(PlayerStatus.ACTIVE, charlieSeatNextHand.playerState!!.status, "Charlie should be ACTIVE now")
        assertTrue(charlieSeatNextHand.playerState!!.holeCards.isNotEmpty(), "Charlie should have cards now")
    }
}
