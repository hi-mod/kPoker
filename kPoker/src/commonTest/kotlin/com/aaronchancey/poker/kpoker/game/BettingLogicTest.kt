package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.evaluation.HandEvaluator
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingLogicTest {

    class DummyEvaluator : HandEvaluator() {
        override fun evaluate(cards: List<Card>): EvaluatedHand = EvaluatedHand(HandRank.HIGH_CARD, cards)
        override fun findBestHand(cards: List<Card>, handSize: Int): List<EvaluatedHand> = listOf(EvaluatedHand(HandRank.HIGH_CARD, cards.take(5)))
        override fun findBestHand(holeCards: List<Card>, communityCards: List<Card>): List<EvaluatedHand> = listOf(EvaluatedHand(HandRank.HIGH_CARD, (holeCards + communityCards).take(5)))
    }

    class TestGame(structure: BettingStructure) : PokerGame(structure, DummyEvaluator()) {
        override val gameVariant: GameVariant = GameVariant.TEXAS_HOLDEM
        override val variantName = "Test"
        override val holeCardCount = 2
        override val usesCommunityCards = true
        override fun dealHoleCards() {}
        override fun evaluateHands(): List<Winner> = emptyList()
    }

    @Test
    fun `test minimum raise updates after a bet`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0)
        val game = TestGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")
        val p3 = Player("3", "Charlie")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(3, PlayerState(p3, 100.0, status = PlayerStatus.WAITING))

        game.initialize(table)
        game.startHand()

        // Observed: Seat 2 (Bob) acts first.
        val state = game.currentState

        // Bob Raises to 10.0.
        // Current Bet 2.0. Raise BY 8. TO 10.
        val actorId = state.currentActor!!.player.id
        game.processAction(Action.Raise(actorId, 8.0, 10.0))

        // Next player acts.
        // Previous raise was TO 10. Previous bet was 2. Raise amount was 8.
        // Min raise to: 10 + 8 = 18.

        val nextRequest = game.getActionRequest()
        assertEquals(8.0, nextRequest?.minimumRaise, "Actual minimumRaise was: ${nextRequest?.minimumRaise}")
    }

    @Test
    fun `test minimum raise updates after a post-flop bet`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0)
        val game = TestGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING)) // Dealer/SB
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING)) // BB

        game.initialize(table)
        game.startHand()

        // Pre-flop.
        // Play until Flop.
        while (game.currentState.phase == GamePhase.PRE_FLOP) {
            val actor = game.currentState.currentActor ?: break
            val toCall = game.currentState.bettingRound!!.currentBet - actor.currentBet
            val amount = minOf(toCall, actor.chips)

            if (toCall > 0) {
                game.processAction(Action.Call(actor.player.id, amount))
            } else {
                game.processAction(Action.Check(actor.player.id))
            }
        }

        // Flop
        // Determine who acts first.
        val actor = game.currentState.currentActor!!

        // Actor Bets 10
        game.processAction(Action.Bet(actor.player.id, 10.0))

        // Next player acts.
        // Current bet is 10.
        // Min raise should be 10.

        val nextRequest = game.getActionRequest()

        assertEquals(10.0, nextRequest?.minimumRaise, "Minimum raise amount should be 10.0")
    }
}
