package com.aaronchancey.poker.network

import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.close
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
}

class PokerWebSocketClient {
    private val client = HttpClient {
        install(WebSockets) {
            contentConverter =
                KotlinxWebsocketSerializationConverter(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    },
                )
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                },
            )
        }
    }

    private var session: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<ServerMessage>(replay = 1)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(replay = 1)
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    suspend fun connect(host: String, port: Int, roomId: String) {
        if (_connectionState.value == ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        try {
            session = client.webSocketSession {
                url.host = host
                url.port = port
                url.encodedPath = "/ws/room/$roomId"
                url.protocol = if (port == 443) URLProtocol.WSS else URLProtocol.WS
            }
            _connectionState.value = ConnectionState.CONNECTED
            startListening()
        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = ConnectionState.DISCONNECTED
            _errors.emit(e)
        }
    }

    private fun startListening() {
        scope.launch {
            val currentSession = session ?: return@launch
            try {
                while (true) {
                    try {
                        val message = currentSession.receiveDeserialized<ServerMessage>()
                        _messages.emit(message)
                    } catch (e: Exception) {
                        _errors.emit(e)
                    }
                }
            } catch (e: Exception) {
                if (currentSession.isActive) {
                    _errors.emit(e)
                }
            } finally {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    suspend fun send(message: ClientMessage) {
        val currentSession = session
        if (currentSession == null || _connectionState.value != ConnectionState.CONNECTED) {
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
        session?.close()
        session = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun close() {
        scope.cancel()
        client.close()
    }
}
