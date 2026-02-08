package com.aaronchancey.poker.kpoker.rake

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.dealing.CardDealer
import com.aaronchancey.poker.kpoker.dealing.DealResult
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.game.VariantStrategy
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemVariant
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RakeIntegrationTest {

    /** Assert doubles are equal within floating-point tolerance. */
    private fun assertChipEquals(expected: Double, actual: Double, message: String? = null) {
        assertTrue(abs(expected - actual) < 0.001, "${message ?: ""} expected:<$expected> but was:<$actual>")
    }

    /**
     * NoOpDealer that doesn't deal actual cards but still marks all eligible
     * players as ACTIVE (matching StandardDealer behavior).
     */
    private class NoOpDealer : CardDealer {
        override fun dealHoleCards(
            state: GameState,
            cardsPerPlayer: Int,
            visibilityPattern: List<CardVisibility>?,
        ): DealResult.HoleCards {
            // Mark all players with chips as ACTIVE (like StandardDealer does)
            var updatedState = state
            for (seat in state.table.seatsWithChips) {
                updatedState = updatedState.withTable(
                    updatedState.table.updateSeat(seat.number) { s ->
                        s.updatePlayerState { ps -> ps.withStatus(PlayerStatus.ACTIVE) }
                    },
                )
            }
            return DealResult.HoleCards(updatedState, emptyMap())
        }

        override fun dealCommunityCards(
            state: GameState,
            count: Int,
            burnFirst: Boolean,
        ): DealResult.CommunityCards = DealResult.CommunityCards(state, emptyList())
    }

    private class TestStrategy : VariantStrategy {
        override val metadata = TexasHoldemVariant
        override val gameVariant = GameVariant.TEXAS_HOLDEM
        override fun evaluateHands(state: GameState): List<Winner> = emptyList()
    }

    private fun createGame(
        rakeCalculator: RakeCalculator? = null,
    ): PokerGame = PokerGame(
        bettingStructure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0),
        variantStrategy = TestStrategy(),
        cardDealer = NoOpDealer(),
        rakeCalculator = rakeCalculator,
    )

    private fun createTableWith3Players(): Table {
        var table = Table.create("1", "Test", 9)
        table = table.sitPlayer(1, PlayerState(Player("1", "Alice"), 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(Player("2", "Bob"), 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(3, PlayerState(Player("3", "Charlie"), 100.0, status = PlayerStatus.WAITING))
        return table
    }

    private fun foldCurrentActor(game: PokerGame) {
        val actorId = game.currentState.currentActor!!.player.id
        game.processAction(Action.Fold(actorId))
    }

    private fun callCurrentActor(game: PokerGame) {
        val actor = game.currentState.currentActor!!
        val round = game.currentState.bettingRound!!
        val amountToCall = round.currentBet - actor.currentBet
        game.processAction(Action.Call(actor.player.id, amountToCall))
    }

    private fun checkCurrentActor(game: PokerGame) {
        val actorId = game.currentState.currentActor!!.player.id
        game.processAction(Action.Check(actorId))
    }

    // === No-Flop-No-Drop Tests ===

    @Test
    fun `preflop fold results in zero rake`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 10.0, minDenomination = 1.0)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Everyone folds preflop - no-flop-no-drop
        foldCurrentActor(game)
        foldCurrentActor(game)

        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase)
        assertEquals(0.0, game.currentState.rake, "Rake should be zero for preflop-only hand")

        // Winner should get full pot (SB + BB = 3.0)
        val winners = game.currentState.winners
        assertEquals(1, winners.size)
        assertEquals(3.0, winners.first().amount)
    }

    // === Post-Flop Rake Tests ===

    @Test
    fun `post-flop fold applies rake with small denomination`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 10.0, minDenomination = 0.1)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Preflop: everyone calls BB of 2, then BB checks
        callCurrentActor(game)
        callCurrentActor(game)
        checkCurrentActor(game)

        // Now on flop. Pot = 6.0. Rake calculated when entering flop.
        assertEquals(GamePhase.FLOP, game.currentState.phase)
        assertChipEquals(0.3, game.currentState.rake)

        // Flop: two players fold → hand ends, rake calculated
        foldCurrentActor(game)
        foldCurrentActor(game)

        // Pot 6.0, rake = floor(0.30 / 0.1) * 0.1 = 0.3
        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase)
        assertChipEquals(0.3, game.currentState.rake)
        assertChipEquals(5.7, game.currentState.winners.first().amount)
    }

    @Test
    fun `post-flop rake with larger pot`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 10.0, minDenomination = 0.1)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Preflop: raise to 10, everyone calls
        val firstActor = game.currentState.currentActor!!.player.id
        game.processAction(Action.Raise(firstActor, 8.0, 10.0))
        callCurrentActor(game)
        callCurrentActor(game)

        // Flop. Pot = 30.0 (3 * 10). Rake not yet calculated.
        assertEquals(GamePhase.FLOP, game.currentState.phase)

        // Flop: two folds → hand ends, rake calculated
        foldCurrentActor(game)
        foldCurrentActor(game)

        // Pot 30.0, rake = floor(1.5 / 0.1) * 0.1 = 1.5
        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase)
        assertEquals(1.5, game.currentState.rake)
        assertEquals(28.5, game.currentState.winners.first().amount)
    }

    // === Running Rake Count Tests ===

    @Test
    fun `rake updates across streets`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 100.0, minDenomination = 0.1)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Preflop: everyone calls (pot = 6)
        callCurrentActor(game)
        callCurrentActor(game)
        checkCurrentActor(game)
        // Rake calculated immediately when entering flop
        assertEquals(GamePhase.FLOP, game.currentState.phase)
        assertChipEquals(0.3, game.currentState.rake)

        // Flop: everyone checks → endBettingRound calculates rake on pot=6
        checkCurrentActor(game)
        checkCurrentActor(game)
        checkCurrentActor(game)

        // After flop betting ends: pot=6, rake = 5% of 6 = 0.3
        assertEquals(GamePhase.TURN, game.currentState.phase)
        assertChipEquals(0.3, game.currentState.rake)

        // Turn: bet 10, others call (pot grows by 30, total 36)
        val turnActor = game.currentState.currentActor!!.player.id
        game.processAction(Action.Bet(turnActor, 10.0))
        callCurrentActor(game)
        callCurrentActor(game)

        // After turn betting ends: pot=36, rake = 5% of 36 = 1.8
        assertEquals(GamePhase.RIVER, game.currentState.phase)
        assertChipEquals(1.8, game.currentState.rake)
    }

    // === Rake Cap Tests ===

    @Test
    fun `rake is capped per pot`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 1.0, minDenomination = 0.1)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Preflop: large raise, everyone calls
        val firstActor = game.currentState.currentActor!!.player.id
        game.processAction(Action.Raise(firstActor, 18.0, 20.0))
        callCurrentActor(game)
        callCurrentActor(game)

        // Flop. Everyone checks to end flop betting.
        assertEquals(GamePhase.FLOP, game.currentState.phase)
        checkCurrentActor(game)
        checkCurrentActor(game)
        checkCurrentActor(game)

        // After flop betting: Pot = 60. 5% = 3.0, but capped at 1.0
        assertEquals(GamePhase.TURN, game.currentState.phase)
        assertEquals(1.0, game.currentState.rake)
    }

    // === No Rake Without Calculator ===

    @Test
    fun `no rake calculator means zero rake always`() {
        val game = createGame(rakeCalculator = null)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Preflop: everyone calls
        callCurrentActor(game)
        callCurrentActor(game)
        checkCurrentActor(game)

        // Flop
        assertEquals(GamePhase.FLOP, game.currentState.phase)
        assertEquals(0.0, game.currentState.rake)

        // Flop: folds end hand
        foldCurrentActor(game)
        foldCurrentActor(game)

        // Winner gets full pot
        assertEquals(6.0, game.currentState.winners.first().amount)
    }

    @Test
    fun `rake resets between hands`() {
        val rake = PercentageRakeCalculator(rakePercent = 0.05, rakeCap = 100.0, minDenomination = 0.1)
        val game = createGame(rakeCalculator = rake)
        game.initialize(createTableWith3Players())
        game.startHand()

        // Play to flop, then fold on flop to end hand with rake
        callCurrentActor(game)
        callCurrentActor(game)
        checkCurrentActor(game)
        assertEquals(GamePhase.FLOP, game.currentState.phase)

        // Fold on flop → endBettingRound calculates rake, then hand ends
        foldCurrentActor(game)
        foldCurrentActor(game)
        assertEquals(GamePhase.HAND_COMPLETE, game.currentState.phase)
        assertTrue(game.currentState.rake > 0.0, "Rake should be positive after post-flop hand")

        // Start new hand - rake should reset
        game.startHand()
        assertEquals(0.0, game.currentState.rake)
    }
}
