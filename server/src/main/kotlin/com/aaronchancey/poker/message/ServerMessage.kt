package com.aaronchancey.poker.message

import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    @Serializable
    @SerialName("welcome")
    data class Welcome(val playerId: PlayerId) : ServerMessage()

    @Serializable
    @SerialName("room_joined")
    data class RoomJoined(val roomInfo: RoomInfo) : ServerMessage()

    @Serializable
    @SerialName("game_state_update")
    data class GameStateUpdate(val state: GameState) : ServerMessage()

    @Serializable
    @SerialName("game_event")
    data class GameEventOccurred(val event: GameEvent) : ServerMessage()

    @Serializable
    @SerialName("action_required")
    data class ActionRequired(val request: ActionRequest) : ServerMessage()

    @Serializable
    @SerialName("error")
    data class Error(val code: String, val message: String) : ServerMessage()

    @Serializable
    @SerialName("player_connected")
    data class PlayerConnected(val playerId: PlayerId, val playerName: String) : ServerMessage()

    @Serializable
    @SerialName("player_disconnected")
    data class PlayerDisconnected(val playerId: PlayerId) : ServerMessage()
}

@Serializable
data class RoomInfo(
    val roomId: String,
    val roomName: String,
    val maxPlayers: Int,
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val minBuyIn: ChipAmount,
    val maxBuyIn: ChipAmount,
    val playerCount: Int,
)
