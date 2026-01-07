package com.aaronchancey.poker.ws

import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.ServerMessage
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PlayerConnection(
    val playerId: PlayerId,
    val playerName: String,
    val session: WebSocketServerSession,
)

class ConnectionManager {
    private val roomConnections = ConcurrentHashMap<String, MutableMap<PlayerId, PlayerConnection>>()
    private val mutex = Mutex()

    suspend fun addConnection(roomId: String, connection: PlayerConnection) {
        mutex.withLock {
            val connections = roomConnections.getOrPut(roomId) { mutableMapOf() }
            connections[connection.playerId] = connection
        }
    }

    suspend fun removeConnection(roomId: String, playerId: PlayerId): PlayerConnection? = mutex.withLock {
        roomConnections[roomId]?.remove(playerId)
    }

    fun getConnection(roomId: String, playerId: PlayerId): PlayerConnection? = roomConnections[roomId]?.get(playerId)

    fun getConnections(roomId: String): List<PlayerConnection> = roomConnections[roomId]?.values?.toList() ?: emptyList()

    suspend fun broadcast(roomId: String, message: ServerMessage) {
        val connections = getConnections(roomId)
        connections.forEach { connection ->
            try {
                connection.session.sendSerialized(message)
            } catch (_: Exception) {
                // Connection might be closed, will be cleaned up elsewhere
            }
        }
    }

    suspend fun sendTo(roomId: String, playerId: PlayerId, message: ServerMessage) {
        val connection = getConnection(roomId, playerId)
        try {
            connection?.session?.sendSerialized(message)
        } catch (_: Exception) {
            // Connection might be closed
        }
    }

    suspend fun broadcastExcept(roomId: String, excludePlayerId: PlayerId, message: ServerMessage) {
        val connections = getConnections(roomId).filter { it.playerId != excludePlayerId }
        connections.forEach { connection ->
            try {
                connection.session.sendSerialized(message)
                println("broadCastExcept Sent message: $message")
            } catch (_: Exception) {
                // Connection might be closed
            }
        }
    }
}
