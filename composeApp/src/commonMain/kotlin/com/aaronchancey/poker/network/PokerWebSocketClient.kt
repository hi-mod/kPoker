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
        currentHost = host
        currentPort = port
        currentRoomId = roomId
        this.playerId = playerId
        shouldBeConnected = true

        if (_connectionState.value == ConnectionState.CONNECTED) return

        scope.launch {
            connectLoop()
        }
    }

    private suspend fun connectLoop() {
        while (shouldBeConnected && currentCoroutineContext().isActive) {
            _connectionState.value = if (hasConnectedBefore) {
                ConnectionState.RECONNECTING
            } else {
                ConnectionState.CONNECTING
            }

            try {
                val host = currentHost ?: return
                val port = currentPort ?: return
                val roomId = currentRoomId ?: return

                session = client.webSocketSession {
                    url.host = host
                    url.port = port
                    url.encodedPath = "/ws/room/$roomId"
                    url.protocol = if (port == 443) URLProtocol.WSS else URLProtocol.WS

                    if (playerId != null) {
                        url.parameters.append("playerId", playerId!!)
                    }
                }

                // Connection successful - reset backoff
                backoffMs = INITIAL_BACKOFF_MS

                // Emit RECONNECTED if this was a reconnection, so ViewModel can auto-rejoin
                _connectionState.value = if (hasConnectedBefore) {
                    ConnectionState.RECONNECTED
                } else {
                    hasConnectedBefore = true
                    ConnectionState.CONNECTED
                }

                listenMessages()
            } catch (e: Exception) {
                println("WebSocket connection failed: ${e.message}")
                _connectionState.value = ConnectionState.RECONNECTING

                // Exponential backoff: 1s → 2s → 4s → 8s → ... → max 30s
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private suspend fun listenMessages() {
        val currentSession = session ?: return
        try {
            while (true) {
                val message = currentSession.receiveDeserialized<ServerMessage>()

                if (message is ServerMessage.Welcome) {
                    playerId = message.playerId
                }

                _messages.emit(message)
            }
        } catch (e: Exception) {
            println("WebSocket disconnected: ${e.message}")
        } finally {
            currentSession.close()
            session = null
            if (shouldBeConnected) {
                _connectionState.value = ConnectionState.RECONNECTING
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
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
