package com.aaronchancey.poker.kpoker.events

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BlindType
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId

sealed class GameEvent {
    abstract val timestamp: Long

    // Game lifecycle
    data class GameInitialized(
        val state: GameState,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class HandStarted(
        val handNumber: Long,
        val dealerSeat: Int,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class HandComplete(
        val winners: List<Winner>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class PhaseChanged(
        val phase: GamePhase,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Player actions
    data class BlindPosted(
        val playerId: PlayerId,
        val amount: ChipAmount,
        val blindType: BlindType,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class ActionTaken(
        val action: Action,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class TurnChanged(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Cards
    data class HoleCardsDealt(
        val playerId: PlayerId,
        val cards: List<Card>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class CommunityCardsDealt(
        val cards: List<Card>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Room events
    data class PlayerJoinedRoom(
        val playerId: PlayerId,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class PlayerLeftRoom(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class PlayerSeated(
        val playerId: PlayerId,
        val seatNumber: Int,
        val buyIn: ChipAmount,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class PlayerStoodUp(
        val playerId: PlayerId,
        val seatNumber: Int,
        val chips: ChipAmount,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class SpectatorJoined(
        val playerId: PlayerId,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    data class SpectatorLeft(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Chat
    data class ChatMessage(
        val playerId: PlayerId,
        val message: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    companion object {
        private fun currentTimeMillis(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }
}
