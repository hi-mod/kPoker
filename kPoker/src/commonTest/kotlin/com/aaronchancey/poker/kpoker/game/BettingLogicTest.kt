package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.dealing.CardDealer
import com.aaronchancey.poker.kpoker.dealing.DealResult
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemVariant
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingLogicTest {

    class NoOpDealer : CardDealer {
        override fun dealHoleCards(state: GameState, cardsPerPlayer: Int, visibilityPattern: List<CardVisibility>?): DealResult.HoleCards = DealResult.HoleCards(state, emptyMap())
        override fun dealCommunityCards(state: GameState, count: Int, burnFirst: Boolean): DealResult.CommunityCards = DealResult.CommunityCards(state, emptyList())
    }

    class TestStrategy : VariantStrategy {
        override val metadata = TexasHoldemVariant
        override val gameVariant = GameVariant.TEXAS_HOLDEM
        override fun evaluateHands(state: GameState): List<Winner> = emptyList()
    }

    private fun createGame(structure: BettingStructure): PokerGame = PokerGame(structure, TestStrategy(), NoOpDealer())

    @Test
    fun `test minimum raise updates after a bet`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0)
        val game = createGame(structure)

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
        val game = createGame(structure)

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

    @Test
    fun `test pot-limit max bet in heads-up preflop`() {
        // Heads-up with 1/2 blinds
        // Total in play = 3 (SB 1 + BB 2)
        // First actor (SB) to act, needs to call 1 to match BB
        // Pot after call = 3 + 1 = 4
        // Max raise = 4
        // Max total bet = 2 + 4 = 6
        val structure = BettingStructure.potLimit(smallBlind = 1.0, bigBlind = 2.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))

        game.initialize(table)
        game.startHand()

        // First actor in heads-up preflop is SB
        val request = game.getActionRequest()!!
        assertEquals(6.0, request.maximumBet, "Pot-limit max bet should be 6 in heads-up preflop with 1/2 blinds")
    }

    @Test
    fun `test pot-limit max bet after raise`() {
        // Heads-up with 1/2 blinds
        // After first actor (SB) raises to 6 (pot), second actor (BB) to act
        // Current bet = 6, BB has 2 in
        // Call amount = 4
        // Total in play after raise = 6 + 2 = 8
        // Pot after BB calls = 8 + 4 = 12
        // Max raise = 12
        // Max total bet = 6 + 12 = 18
        val structure = BettingStructure.potLimit(smallBlind = 1.0, bigBlind = 2.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))

        game.initialize(table)
        game.startHand()

        // First actor raises to 6 (pot)
        val firstActor = game.currentState.currentActor!!
        game.processAction(Action.Raise(firstActor.player.id, amount = 4.0, totalBet = 6.0))

        // Second actor (BB) to act
        val request = game.getActionRequest()!!
        assertEquals(18.0, request.maximumBet, "Pot-limit max should be 18 after first actor pots it")
    }
}
