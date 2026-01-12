package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PotCollectionTest {

    private fun createGame(): TexasHoldemGame = TexasHoldemGame.noLimit(smallBlind = 1.0, bigBlind = 2.0)

    private fun createTableWithPlayers(): Table {
        var table = Table.create("1", "Test Table", 6)
        val alice = Player("p1", "Alice")
        val bob = Player("p2", "Bob")

        // Seat 1: Alice (100 chips)
        table = table.sitPlayer(1, PlayerState(alice, chips = 100.0))

        // Seat 2: Bob (100 chips)
        table = table.sitPlayer(2, PlayerState(bob, chips = 100.0))

        return table
    }

    @Test
    fun testBlindsNotCollectedImmediately() {
        val game = createGame()
        val table = createTableWithPlayers()
        game.initialize(table)

        // Start Hand
        // Alice (Seat 1) should be Dealer (first active seat usually)
        // With 2 players:
        // Dealer posts Small Blind? In Heads Up, Dealer is SB.
        // Let's see standard rules in code.
        // advanceDealer() -> if Seat 1 is dealer.
        // postBlinds() -> SB is dealer+1, BB is dealer+2.
        // With 2 players: 1 (Dealer), 2.
        // SB = (Index 0 + 1) % 2 = Index 1 (Seat 2).
        // BB = (Index 0 + 2) % 2 = Index 0 (Seat 1).
        // Wait, Heads up rules are often different (Dealer is SB).
        // But let's check what the code does.
        // `advanceDealer` likely sets seat 1 as dealer first time.

        game.startHand()
        val state = game.currentState

        // Check Phase
        // It advances to PRE_FLOP after startHand()
        assertEquals(GamePhase.PRE_FLOP, state.phase)

        // Verify Pot is EMPTY (or close to it)
        // Before fix: Pot would have 3 chips (1+2).
        // After fix: Pot should be 0 because bets are still with players.
        assertEquals(0.0, state.potManager.totalPot, "Pot should be empty at start of pre-flop")

        // Verify Players have bets in front of them
        val seat1 = state.table.getSeat(1)?.playerState!!
        val seat2 = state.table.getSeat(2)?.playerState!!

        // Depending on dealer button, one is SB, one is BB.
        // In 2 player game code:
        // dealerIndex=0 (Seat 1).
        // SB Index = (0+1)%2 = 1 (Seat 2).
        // BB Index = (0+2)%2 = 0 (Seat 1).
        // So Seat 2 is SB (1), Seat 1 is BB (2).

        if (seat1.isBigBlind) {
            assertEquals(2.0, seat1.totalBetThisRound, "BB (Seat 1) should have 2 bet")
            assertEquals(1.0, seat2.totalBetThisRound, "SB (Seat 2) should have 1 bet")
        } else {
            assertEquals(1.0, seat1.totalBetThisRound, "SB (Seat 1) should have 1 bet")
            assertEquals(2.0, seat2.totalBetThisRound, "BB (Seat 2) should have 2 bet")
        }
    }

    @Test
    fun testPotCollectionAfterRound() {
        val game = createGame()
        val table = createTableWithPlayers()
        game.initialize(table)
        game.startHand()

        var state = game.currentState

        // Identify SB and BB
        val sb = state.table.occupiedSeats.find { it.playerState?.isSmallBlind == true }!!.playerState!!.player
        val bb = state.table.occupiedSeats.find { it.playerState?.isBigBlind == true }!!.playerState!!.player

        game.processAction(Action.Call(sb.id, 1.0))

        // Action 2: BB Checks.
        game.processAction(Action.Check(bb.id))

        state = game.currentState

        // Round should end. Phase should be FLOP.
        assertEquals(GamePhase.FLOP, state.phase)

        // Pot should now have collected the bets.
        // SB: 1 (blind) + 1 (call) = 2.
        // BB: 2 (blind) + 0 (check) = 2.
        // Total Pot = 4.
        assertEquals(4.0, state.potManager.totalPot)

        // Crucial: Should be ONE main pot, not two split pots.
        assertEquals(1, state.potManager.pots.size, "Should be exactly one pot")
        assertTrue(state.potManager.pots[0].isMain, "Pot should be main")
        assertEquals(4.0, state.potManager.pots[0].amount, "Main pot should have 4 chips")
    }

    @Test
    fun testPotAccumulationAcrossRounds() {
        val game = createGame()
        val table = createTableWithPlayers()
        game.initialize(table)
        game.startHand()

        // Identify SB and BB
        var state = game.currentState
        val sb = state.table.occupiedSeats.find { it.playerState?.isSmallBlind == true }!!.playerState!!.player
        val bb = state.table.occupiedSeats.find { it.playerState?.isBigBlind == true }!!.playerState!!.player

        // Pre-Flop: SB Call, BB Check
        game.processAction(Action.Call(sb.id, 1.0))

        // BB checks
        game.processAction(Action.Check(bb.id))

        state = game.currentState
        assertEquals(GamePhase.FLOP, state.phase)
        assertEquals(4.0, state.potManager.totalPot, "Pot should be 4 after Pre-Flop")

        // Verify totalBetThisRound is reset
        state.table.occupiedSeats.forEach { seat ->
            assertEquals(0.0, seat.playerState!!.totalBetThisRound, "totalBetThisRound should be 0 for Seat ${seat.number}")
        }

        // Flop: Check, Check
        // BB acts first Post-Flop in Heads-Up
        game.processAction(Action.Check(bb.id))

        // SB acts second
        game.processAction(Action.Check(sb.id))

        state = game.currentState
        assertEquals(GamePhase.TURN, state.phase)

        // Pot should still be 4, NOT 8
        assertEquals(4.0, state.potManager.totalPot, "Pot should remain 4 after Flop checks")
    }
}
