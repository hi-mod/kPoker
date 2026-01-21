package com.aaronchancey.poker.network

import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Connection state for the WebSocket client.
 *
 * State transitions:
 * - DISCONNECTED → CONNECTING (initial connect)
 * - CONNECTING → CONNECTED (success) or RECONNECTING (failure)
 * - CONNECTED → RECONNECTING (connection lost)
 * - RECONNECTING → RECONNECTED (success) or stays RECONNECTING (retry)
 * - RECONNECTED → CONNECTED (after auto-rejoin handled by ViewModel)
 * - Any → DISCONNECTED (intentional disconnect)
 */
enum class ConnectionState {
    /** Not connected and not trying to connect */
    DISCONNECTED,

    /** Initial connection attempt */
    CONNECTING,

    /** Successfully connected */
    CONNECTED,

    /** Lost connection, attempting to reconnect */
    RECONNECTING,

    /** Successfully reconnected after connection loss - signals ViewModel to auto-rejoin */
    RECONNECTED,
}

class PokerWebSocketClient(
    private val client: HttpClient,
) {

    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<ServerMessage>(replay = 1)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(replay = 1)
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    private var playerId: String? = null
    private var currentHost: String? = null
    private var currentPort: Int? = null
    private var currentRoomId: String? = null
    private var shouldBeConnected = false

    /** Tracks if this is a reconnection attempt (had a previous successful connection) */
    private var hasConnectedBefore = false

    /** Current backoff delay in milliseconds */
    private var backoffMs = INITIAL_BACKOFF_MS

    fun connect(host: String, port: Int, roomId: String, playerId: PlayerId) {
        println("[WebSocketClient] connect: host=$host, port=$port, roomId=$roomId, playerId=$playerId")
        currentHost = host
        currentPort = port
        currentRoomId = roomId
        this.playerId = playerId
        shouldBeConnected = true

        if (_connectionState.value == ConnectionState.CONNECTED) {
            println("[WebSocketClient] connect: Already connected, skipping")
            return
        }

        scope.launch {
            connectLoop()
        }
    }

    private suspend fun connectLoop() {
        println("[WebSocketClient] connectLoop: Starting, hasConnectedBefore=$hasConnectedBefore")
        while (shouldBeConnected && currentCoroutineContext().isActive) {
            val newState = if (hasConnectedBefore) ConnectionState.RECONNECTING else ConnectionState.CONNECTING
            println("[WebSocketClient] connectLoop: Setting state to $newState")
            _connectionState.value = newState

            try {
                val host = currentHost ?: run {
                    println("[WebSocketClient] connectLoop: No host configured, exiting")
                    return
                }
                val port = currentPort ?: run {
                    println("[WebSocketClient] connectLoop: No port configured, exiting")
                    return
                }
                val roomId = currentRoomId ?: run {
                    println("[WebSocketClient] connectLoop: No roomId configured, exiting")
                    return
                }

                val protocol = if (port == 443) URLProtocol.WSS else URLProtocol.WS
                val wsUrl = "${protocol.name.lowercase()}://$host:$port/ws/room/$roomId?playerId=$playerId"
                println("[WebSocketClient] connectLoop: Connecting to $wsUrl")

                session = client.webSocketSession {
                    url.host = host
                    url.port = port
                    url.encodedPath = "/ws/room/$roomId"
                    url.protocol = protocol

                    if (playerId != null) {
                        url.parameters.append("playerId", playerId!!)
                    }
                }

                // Connection successful - reset backoff
                backoffMs = INITIAL_BACKOFF_MS
                println("[WebSocketClient] connectLoop: WebSocket session established")

                // Emit RECONNECTED if this was a reconnection, so ViewModel can auto-rejoin
                val connectedState = if (hasConnectedBefore) {
                    ConnectionState.RECONNECTED
                } else {
                    hasConnectedBefore = true
                    ConnectionState.CONNECTED
                }
                println("[WebSocketClient] connectLoop: Setting state to $connectedState")
                _connectionState.value = connectedState

                listenMessages()
            } catch (e: Exception) {
                println("[WebSocketClient] connectLoop: Connection FAILED - ${e::class.simpleName}: ${e.message}")
                _connectionState.value = ConnectionState.RECONNECTING

                // Exponential backoff: 1s → 2s → 4s → 8s → ... → max 30s
                println("[WebSocketClient] connectLoop: Waiting ${backoffMs}ms before retry")
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
        println("[WebSocketClient] connectLoop: Exiting, shouldBeConnected=$shouldBeConnected")
    }

    private suspend fun listenMessages() {
        val currentSession = session ?: run {
            println("[WebSocketClient] listenMessages: No session, exiting")
            return
        }
        println("[WebSocketClient] listenMessages: Starting message loop")
        try {
            while (true) {
                val message = currentSession.receiveDeserialized<ServerMessage>()

                if (message is ServerMessage.Welcome) {
                    val oldPlayerId = playerId
                    playerId = message.playerId
                    println("[WebSocketClient] listenMessages: Received Welcome - serverPlayerId=${message.playerId}, wasUsingPlayerId=$oldPlayerId")
                } else if (message is ServerMessage.Error) {
                    println("[WebSocketClient] listenMessages: Received Error - code=${message.code}, message=${message.message}")
                } else if (message is ServerMessage.RoomJoined) {
                    println("[WebSocketClient] listenMessages: Received RoomJoined - roomName=${message.roomInfo.roomName}")
                }

                _messages.emit(message)
            }
        } catch (e: Exception) {
            println("[WebSocketClient] listenMessages: Disconnected - ${e::class.simpleName}: ${e.message}")
        } finally {
            currentSession.close()
            session = null
            val finalState = if (shouldBeConnected) ConnectionState.RECONNECTING else ConnectionState.DISCONNECTED
            println("[WebSocketClient] listenMessages: Session closed, setting state to $finalState")
            _connectionState.value = finalState
        }
    }

    /**
     * Acknowledges that the ViewModel has handled the RECONNECTED state.
     * Transitions from RECONNECTED → CONNECTED.
     */
    fun acknowledgeReconnected() {
        if (_connectionState.value == ConnectionState.RECONNECTED) {
            _connectionState.value = ConnectionState.CONNECTED
        }
    }

    suspend fun send(message: ClientMessage) {
        val currentSession = session
        if (currentSession == null) {
            _errors.emit(IllegalStateException("Not connected"))
            return
        }

        try {
            currentSession.sendSerialized(message)
            println("Sent message: $message")
        } catch (e: Exception) {
            _errors.emit(e)
        }
    }

    suspend fun disconnect() {
        shouldBeConnected = false
        hasConnectedBefore = false
        backoffMs = INITIAL_BACKOFF_MS
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun closeConnection() {
        scope.launch {
            disconnect()
        }
    }

    fun close() {
        scope.cancel()
        client.close()
    }

    companion object {
        private const val INITIAL_BACKOFF_MS = 1000L
        private const val MAX_BACKOFF_MS = 30_000L
    }
}
