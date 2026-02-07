package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ActionType
import com.aaronchancey.poker.kpoker.betting.BettingType
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.kpoker.player.Seat
import com.aaronchancey.poker.kpoker.player.Table
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ActionEvProviderTest {
    private val provider = ActionEvProvider()
    private val heroId = "hero"
    private val villainId = "villain"

    private fun cards(vararg s: String) = s.map { Card.fromString(it) }

    private fun createGameState(
        heroCards: List<Card>,
        communityCards: List<Card> = emptyList(),
        villainStatus: PlayerStatus = PlayerStatus.ACTIVE,
    ): GameState {
        val hero = PlayerState(
            player = Player(id = heroId, name = "Hero"),
            chips = 1000.0,
            holeCards = heroCards,
            status = PlayerStatus.ACTIVE,
        )
        val villain = PlayerState(
            player = Player(id = villainId, name = "Villain"),
            chips = 1000.0,
            holeCards = emptyList(),
            status = villainStatus,
        )
        val seats = listOf(
            Seat(number = 1, playerState = hero),
            Seat(number = 2, playerState = villain),
        ) + (3..6).map { Seat(number = it) }
        return GameState(
            table = Table(id = "test", name = "Test", seats = seats),
            variant = GameVariant.TEXAS_HOLDEM,
            communityCards = communityCards,
        )
    }

    private fun actionRequest(canCheck: Boolean, canCall: Boolean, amountToCall: Double = 10.0) = ActionRequest(
        playerId = heroId,
        validActions = buildSet {
            add(ActionType.FOLD)
            if (canCheck) add(ActionType.CHECK)
            if (canCall) add(ActionType.CALL)
        },
        minimumBet = 0.0,
        minimumRaise = 0.0,
        maximumBet = 0.0,
        amountToCall = amountToCall,
        minimumDenomination = 0.5,
        bettingType = BettingType.NO_LIMIT,
    )

    @Test
    fun returnsNullForNullGameState() = runTest {
        val result = provider.getActionEv(
            gameState = null,
            playerId = heroId,
            availableActions = null,
        )
        assertNull(result)
    }

    @Test
    fun returnsNullForNullPlayerId() = runTest {
        val gs = createGameState(heroCards = cards("As", "Ah"))
        val result = provider.getActionEv(
            gameState = gs,
            playerId = null,
            availableActions = null,
        )
        assertNull(result)
    }

    @Test
    fun returnsNullWhenPlayerHasNoCards() = runTest {
        val gs = createGameState(heroCards = emptyList())
        val result = provider.getActionEv(
            gameState = gs,
            playerId = heroId,
            availableActions = null,
        )
        assertNull(result)
    }

    @Test
    fun returnsNullWhenNoOpponents() = runTest {
        // Only hero in hand (villain is FOLDED)
        val gs = createGameState(
            heroCards = cards("As", "Ah"),
            villainStatus = PlayerStatus.FOLDED,
        )
        val result = provider.getActionEv(
            gameState = gs,
            playerId = heroId,
            availableActions = null,
        )
        assertNull(result)
    }

    @Test
    fun returnsValidEvForStrongHand() = runTest {
        val gs = createGameState(heroCards = cards("As", "Ah"))
        val actions = actionRequest(canCheck = false, canCall = true, amountToCall = 10.0)
        val result = provider.getActionEv(
            gameState = gs,
            playerId = heroId,
            availableActions = actions,
        )
        assertNotNull(result)
        assertTrue(result.equity > 0.70, "AA equity should be > 70%, was ${result.equity}")
        assertNotNull(result.callEv)
        assertNull(result.checkEv)
    }

    @Test
    fun returnsCheckEvWhenCheckAvailable() = runTest {
        val gs = createGameState(heroCards = cards("As", "Ah"))
        val actions = actionRequest(canCheck = true, canCall = false)
        val result = provider.getActionEv(
            gameState = gs,
            playerId = heroId,
            availableActions = actions,
        )
        assertNotNull(result)
        assertNotNull(result.checkEv)
        assertNull(result.callEv)
    }
}
