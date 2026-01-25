package com.aaronchancey.poker.network

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Repository managing poker room session state and WebSocket communication.
 *
 * This repository is "self-activating": it internally processes messages from the WebSocket
 * client and updates unified session state, eliminating the need for ViewModels to collect
 * message flows just to trigger side effects.
 *
 * @property client The WebSocket client for server communication
 */
class PokerRepository(
    private val client: PokerWebSocketClient,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _session = MutableStateFlow(RoomSession.Initial)
    val session: StateFlow<RoomSession> = _session.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents: SharedFlow<GameEvent> = _gameEvents.asSharedFlow()

    val connectionState = client.connectionState

    /**
     * Starts internal message processing.
     *
     * This must be called after connecting to begin updating session state.
     * The processing runs in the repository's own scope and continues until [close] is called.
     */
    fun startMessageProcessing() {
        scope.launch {
            client.messages.collect { message ->
                handleServerMessage(message)
            }
        }
        scope.launch {
            client.errors.collect { error ->
                _session.update { it.copy(error = error.message) }
            }
        }
    }

    private fun handleServerMessage(message: ServerMessage) {
        println("Received message: $message")
        when (message) {
            is ServerMessage.Welcome -> {
                _session.update { it.copy(playerId = message.playerId) }
            }

            is ServerMessage.RoomJoined -> {
                _session.update { it.copy(roomInfo = message.roomInfo) }
            }

            is ServerMessage.GameStateUpdate -> {
                _session.update { current ->
                    val updatedRoomInfo = current.roomInfo?.copy(
                        playerCount = message.state.table.playerCount,
                    )
                    val clearActions = message.state.currentActor?.player?.id != current.playerId
                    current.copy(
                        gameState = message.state,
                        roomInfo = updatedRoomInfo ?: current.roomInfo,
                        availableActions = if (clearActions) null else current.availableActions,
                    )
                }
            }

            is ServerMessage.GameEventOccurred -> {
                scope.launch { _gameEvents.emit(message.event) }
                when (message.event) {
                    is GameEvent.HandComplete -> {
                        _session.update { it.copy(availableActions = null) }
                    }

                    is GameEvent.HandStarted -> {
                        _session.update { it.copy(showdown = null) }
                    }

                    else -> { /* No state change needed */ }
                }
            }

            is ServerMessage.ActionRequired -> {
                _session.update { it.copy(availableActions = message.request) }
            }

            is ServerMessage.Error -> {
                _session.update { it.copy(error = "${message.code}: ${message.message}") }
            }

            is ServerMessage.PlayerConnected -> {
                // Could show notification
            }

            is ServerMessage.PlayerDisconnected -> {
                // Could show notification
            }

            is ServerMessage.ShowdownRequired -> {
                _session.update { it.copy(showdown = message.request) }
            }
        }
    }

    fun connect(host: String, port: Int, roomId: String, playerId: PlayerId) {
        client.connect(host, port, roomId, playerId)
        startMessageProcessing()
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
        _session.value = RoomSession.Initial
    }

    fun clearError() {
        _session.update { it.copy(error = null) }
    }

    /**
     * Acknowledges that the RECONNECTED state has been handled.
     * Call this after auto-rejoining the room.
     */
    fun acknowledgeReconnected() {
        client.acknowledgeReconnected()
    }

    fun close() {
        scope.cancel()
        client.closeConnection()
    }
}
