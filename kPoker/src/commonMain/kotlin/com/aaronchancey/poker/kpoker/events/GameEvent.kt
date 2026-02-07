package com.aaronchancey.poker.kpoker.events

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BlindType
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GameEvent {
    abstract val timestamp: Long

    // Game lifecycle
    @Serializable
    @SerialName("game_initialized")
    data class GameInitialized(
        val state: GameState,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("hand_started")
    data class HandStarted(
        val handNumber: Long,
        val dealerSeat: Int,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("hand_complete")
    data class HandComplete(
        val winners: List<Winner>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("phase_changed")
    data class PhaseChanged(
        val phase: GamePhase,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Player actions
    @Serializable
    @SerialName("blind_posted")
    data class BlindPosted(
        val playerId: PlayerId,
        val amount: ChipAmount,
        val blindType: BlindType,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("action_taken")
    data class ActionTaken(
        val action: Action,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("turn_changed")
    data class TurnChanged(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Cards
    @Serializable
    @SerialName("hole_cards_dealt")
    data class HoleCardsDealt(
        val playerId: PlayerId,
        val cards: List<Card>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("community_cards_dealt")
    data class CommunityCardsDealt(
        val cards: List<Card>,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Room events
    @Serializable
    @SerialName("player_joined_room")
    data class PlayerJoinedRoom(
        val playerId: PlayerId,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("player_left_room")
    data class PlayerLeftRoom(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("player_seated")
    data class PlayerSeated(
        val playerId: PlayerId,
        val seatNumber: Int,
        val buyIn: ChipAmount,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("player_stood_up")
    data class PlayerStoodUp(
        val playerId: PlayerId,
        val seatNumber: Int,
        val chips: ChipAmount,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("spectator_joined")
    data class SpectatorJoined(
        val playerId: PlayerId,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("spectator_left")
    data class SpectatorLeft(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Sit-out
    @Serializable
    @SerialName("player_sat_out")
    data class PlayerSatOut(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    @Serializable
    @SerialName("player_sat_in")
    data class PlayerSatIn(
        val playerId: PlayerId,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    // Chat
    @Serializable
    @SerialName("chat_message")
    data class ChatMessage(
        val playerId: PlayerId,
        val message: String,
        override val timestamp: Long = currentTimeMillis(),
    ) : GameEvent()

    companion object {
        private fun currentTimeMillis(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }
}
