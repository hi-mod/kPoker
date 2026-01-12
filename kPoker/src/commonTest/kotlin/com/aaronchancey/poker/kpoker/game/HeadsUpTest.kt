package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HeadsUpTest {

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
    fun testHeadsUpBlindsAssignment() {
        val game = createGame()
        val table = createHeadsUpTable()
        game.initialize(table)

        // Hand 1: Dealer should be SB
        game.startHand()
        val state = game.currentState
        val dealerSeat = state.dealerSeatNumber

        val dealerState = state.table.getSeat(dealerSeat)?.playerState!!
        val nonDealerState = state.table.occupiedSeats.find { it.number != dealerSeat }?.playerState!!

        assertTrue(dealerState.isDealer, "Dealer should be marked as dealer")

        // In professional Heads-Up, Dealer is SB, Non-Dealer is BB
        assertEquals(true, dealerState.isSmallBlind, "Dealer should be Small Blind in Heads-Up")
        assertEquals(true, nonDealerState.isBigBlind, "Non-Dealer should be Big Blind in Heads-Up")

        assertEquals(1.0, dealerState.currentBet, "Dealer (SB) should have posted 1")
        assertEquals(2.0, nonDealerState.currentBet, "Non-Dealer (BB) should have posted 2")
    }

    @Test
    fun testHeadsUpActionOrder() {
        val game = createGame()
        val table = createHeadsUpTable()
        game.initialize(table)
        game.startHand()

        val state = game.currentState

        // Pre-flop: SB (Dealer) acts first
        assertEquals(state.dealerSeatNumber, state.currentActorSeatNumber, "Dealer (SB) should act first pre-flop in Heads-Up")
    }

    @Test
    fun testHeadsUpPostFlopActionOrder() {
        val game = createGame()
        val table = createHeadsUpTable()
        game.initialize(table)
        game.startHand()

        // Dealer (SB) calls 1 (total 2)
        val dealerSeat = game.currentState.dealerSeatNumber
        val sb = game.currentState.table.getSeat(dealerSeat)!!.playerState!!.player
        game.processAction(Action.Call(sb.id, 1.0))

        // Non-Dealer (BB) checks
        val nonDealerSeat = game.currentState.table.occupiedSeats.find { it.number != dealerSeat }!!.number
        val bb = game.currentState.table.getSeat(nonDealerSeat)!!.playerState!!.player
        game.processAction(Action.Check(bb.id))

        // Now we are at FLOP
        val state = game.currentState
        assertEquals(GamePhase.FLOP, state.phase)

        // Post-flop: BB (Non-Dealer) should act first
        // Current actor should be Non-Dealer
        assertEquals(nonDealerSeat, state.currentActorSeatNumber, "Non-Dealer (BB) should act first post-flop in Heads-Up")
    }
}
