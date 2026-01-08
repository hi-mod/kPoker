package com.aaronchancey.poker.network

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.RoomInfo
import com.aaronchancey.poker.shared.message.ServerMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach

class PokerRepository(
    private val client: PokerWebSocketClient = PokerWebSocketClient(),
) {
    private val _playerId = MutableStateFlow<PlayerId?>(null)
    val playerId: StateFlow<PlayerId?> = _playerId.asStateFlow()

    private val _roomInfo = MutableStateFlow<RoomInfo?>(null)
    val roomInfo: StateFlow<RoomInfo?> = _roomInfo.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _availableActions = MutableStateFlow<ActionRequest?>(null)
    val availableActions: StateFlow<ActionRequest?> = _availableActions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val connectionState = client.connectionState

    val messages: Flow<ServerMessage> = client.messages.onEach { message ->
        handleServerMessage(message)
    }

    val errors: Flow<Throwable> = client.errors.onEach { error ->
        _error.value = error.message
    }

    private fun handleServerMessage(message: ServerMessage) {
        println("Received message: $message")
        when (message) {
            is ServerMessage.Welcome -> {
                _playerId.value = message.playerId
            }

            is ServerMessage.RoomJoined -> {
                _roomInfo.value = message.roomInfo
            }

            is ServerMessage.GameStateUpdate -> {
                _gameState.value = message.state
                _roomInfo.value?.let { currentInfo ->
                    _roomInfo.value = currentInfo.copy(
                        playerCount = message.state.table.playerCount,
                    )
                }
                if (message.state.currentActor?.player?.id != playerId.value) {
                    _availableActions.value = null
                }
            }

            is ServerMessage.GameEventOccurred -> {
                if (message.event is GameEvent.HandComplete) {
                    _availableActions.value = null
                }
            }

            is ServerMessage.ActionRequired -> {
                _availableActions.value = message.request
            }

            is ServerMessage.Error -> {
                _error.value = "${message.code}: ${message.message}"
            }

            is ServerMessage.PlayerConnected -> {
                // Could show notification
            }

            is ServerMessage.PlayerDisconnected -> {
                // Could show notification
            }
        }
    }

    suspend fun connect(host: String, port: Int, roomId: String) {
        client.connect(host, port, roomId)
    }

    suspend fun joinRoom(playerName: String) {
        client.send(ClientMessage.JoinRoom(playerName))
    }

    suspend fun leaveRoom() {
        client.send(ClientMessage.LeaveRoom)
    }

    suspend fun takeSeat(seatNumber: Int, buyIn: ChipAmount) {
        client.send(ClientMessage.TakeSeat(seatNumber, buyIn))
    }

    suspend fun leaveSeat() {
        client.send(ClientMessage.LeaveSeat)
    }

    suspend fun performAction(action: Action) {
        client.send(ClientMessage.PerformAction(action))
    }

    suspend fun sendChat(message: String) {
        client.send(ClientMessage.SendChat(message))
    }

    suspend fun disconnect() {
        client.disconnect()
        _playerId.value = null
        _roomInfo.value = null
        _gameState.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
