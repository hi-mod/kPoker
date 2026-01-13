package com.aaronchancey.poker.network

import com.aaronchancey.poker.di.AppModule
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
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

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

class PokerWebSocketClient {

    private val client = AppModule.httpClient
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
            _connectionState.value = if (playerId == null) ConnectionState.CONNECTING else ConnectionState.RECONNECTING

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

                _connectionState.value = ConnectionState.CONNECTED

                // If we are reconnecting (have a playerId), we might need to re-join automatically
                // But the server handles re-sending state on JoinRoom.
                // The client app likely watches connectionState and re-sends JoinRoom if needed.
                // However, capturing the Welcome message is crucial.

                listenMessages()
            } catch (_: Exception) {
                // Connection failed
                _connectionState.value = ConnectionState.RECONNECTING // or DISCONNECTED
                delay(3000) // Wait before retrying
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
        } catch (_: Exception) {
            // Disconnected
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

    suspend fun send(message: ClientMessage) {
        val currentSession = session
        if (currentSession == null) {
            // Allow sending if session exists even if state update lags slightly
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
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}
