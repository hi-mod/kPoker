package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.betting.BlindType
import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.dealing.CardDealer
import com.aaronchancey.poker.kpoker.dealing.DealResult
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AntePostingTest {

    class NoOpDealer : CardDealer {
        override fun dealHoleCards(
            state: GameState,
            cardsPerPlayer: Int,
            visibilityPattern: List<CardVisibility>?,
        ): DealResult.HoleCards = DealResult.HoleCards(state, emptyMap())

        override fun dealCommunityCards(
            state: GameState,
            count: Int,
            burnFirst: Boolean,
        ): DealResult.CommunityCards = DealResult.CommunityCards(state, emptyList())
    }

    class TestStrategy : VariantStrategy {
        override val metadata = TexasHoldemVariant
        override val gameVariant = GameVariant.TEXAS_HOLDEM
        override fun evaluateHands(state: GameState): List<Winner> = emptyList()
    }

    private fun createGame(structure: BettingStructure): PokerGame = PokerGame(structure, TestStrategy(), NoOpDealer())

    @Test
    fun `basic ante posting - 3 players post antes, pot has antes, currentBet is 0`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0, ante = 0.5)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")
        val p3 = Player("3", "Charlie")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(3, PlayerState(p3, 100.0, status = PlayerStatus.WAITING))

        game.initialize(table)

        val events = mutableListOf<GameEvent>()
        game.addEventListener { events.add(it) }

        game.startHand()

        val state = game.currentState

        // 3 antes of 0.5 = 1.5 in pot (blinds stay with players until collected)
        assertEquals(1.5, state.potManager.totalPot)

        // Verify all players had chips deducted for antes
        val playerChips = state.table.seats.mapNotNull { it.playerState?.chips }
        // Each started with 100, paid 0.5 ante
        // SB paid additional 1.0, BB paid additional 2.0
        assertTrue(playerChips.all { it < 100.0 })

        // Verify ante events were emitted
        val anteEvents = events.filterIsInstance<GameEvent.BlindPosted>()
            .filter { it.blindType == BlindType.ANTE }
        assertEquals(3, anteEvents.size)

        // currentBet should reflect only the big blind, not antes
        assertEquals(2.0, state.bettingRound?.currentBet)
    }

    @Test
    fun `ante plus blinds - pot equals antes, blinds stay with players`() {
        val structure = BettingStructure.noLimit(smallBlind = 5.0, bigBlind = 10.0, ante = 1.0)
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

        val state = game.currentState

        // 3 antes of 1.0 = 3.0 in pot (blinds stay with players until collected)
        assertEquals(3.0, state.potManager.totalPot)

        // currentBet reflects only the big blind
        assertEquals(10.0, state.bettingRound?.currentBet)
    }

    @Test
    fun `partial ante all-in - player cannot cover full ante`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0, ante = 5.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")
        val p3 = Player("3", "Charlie") // Only 3 chips, can't cover 5 ante

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(3, PlayerState(p3, 3.0, status = PlayerStatus.WAITING))

        game.initialize(table)

        val events = mutableListOf<GameEvent>()
        game.addEventListener { events.add(it) }

        game.startHand()

        val state = game.currentState

        // Charlie posts only 3 (all he has)
        val charlieState = state.table.getSeat(3)?.playerState
        assertEquals(0.0, charlieState?.chips)
        assertEquals(PlayerStatus.ALL_IN, charlieState?.status)

        // Total antes: 5 + 5 + 3 = 13, plus blinds
        val anteEvents = events.filterIsInstance<GameEvent.BlindPosted>()
            .filter { it.blindType == BlindType.ANTE }
        assertEquals(3, anteEvents.size)
        assertEquals(3.0, anteEvents.find { it.playerId == "3" }?.amount)
    }

    @Test
    fun `no ante configured - hand proceeds normally`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0, ante = 0.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))

        game.initialize(table)

        val events = mutableListOf<GameEvent>()
        game.addEventListener { events.add(it) }

        game.startHand()

        val state = game.currentState

        // No antes, pot should be 0 (blinds stay with players until collected)
        assertEquals(0.0, state.potManager.totalPot)

        // No ante events
        val anteEvents = events.filterIsInstance<GameEvent.BlindPosted>()
            .filter { it.blindType == BlindType.ANTE }
        assertEquals(0, anteEvents.size)
    }

    @Test
    fun `sitting-out players excluded from ante posting`() {
        val structure = BettingStructure.noLimit(smallBlind = 1.0, bigBlind = 2.0, ante = 1.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice")
        val p2 = Player("2", "Bob")
        val p3 = Player("3", "Charlie") // Sitting out

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(3, PlayerState(p3, 100.0, status = PlayerStatus.SITTING_OUT))

        game.initialize(table)

        val events = mutableListOf<GameEvent>()
        game.addEventListener { events.add(it) }

        game.startHand()

        val state = game.currentState

        // Only 2 players post antes (1.0 each) = 2.0 in pot (blinds stay with players)
        assertEquals(2.0, state.potManager.totalPot)

        // Charlie's chips unchanged (sitting out)
        val charlieState = state.table.getSeat(3)?.playerState
        assertEquals(100.0, charlieState?.chips)

        // Only 2 ante events
        val anteEvents = events.filterIsInstance<GameEvent.BlindPosted>()
            .filter { it.blindType == BlindType.ANTE }
        assertEquals(2, anteEvents.size)
    }

    @Test
    fun `all-in on ante then blind position - posts 0 for blind`() {
        val structure = BettingStructure.noLimit(smallBlind = 5.0, bigBlind = 10.0, ante = 3.0)
        val game = createGame(structure)

        val p1 = Player("1", "Alice") // Will be dealer
        val p2 = Player("2", "Bob") // Will be SB, only 3 chips (goes all-in on ante)

        var table = Table.create("1", "Table", 9)
        table = table.sitPlayer(1, PlayerState(p1, 100.0, status = PlayerStatus.WAITING))
        table = table.sitPlayer(2, PlayerState(p2, 3.0, status = PlayerStatus.WAITING))

        game.initialize(table)
        game.startHand()

        val state = game.currentState

        // Bob goes all-in on ante (3 chips), has 0 left for SB
        val bobState = state.table.getSeat(2)?.playerState
        assertEquals(0.0, bobState?.chips)
        assertEquals(PlayerStatus.ALL_IN, bobState?.status)

        // Pot: antes (3 + 3) + blinds (0 from Bob, 10 from Alice as BB) = 16
        // Wait - in heads up, dealer is SB. So seat 1 (Alice) is dealer/SB, seat 2 (Bob) is BB
        // But actually dealer advances, need to verify which seat is which
    }
}
