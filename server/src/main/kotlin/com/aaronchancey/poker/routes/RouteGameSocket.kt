package com.aaronchancey.poker.routes

import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.room.ServerRoom
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
import com.aaronchancey.poker.ws.ConnectionManager
import com.aaronchancey.poker.ws.PlayerConnection
import io.ktor.server.application.log
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.UUID
import kotlinx.coroutines.channels.ClosedReceiveChannelException

fun Route.routeGameSocket() {
    val roomManager by lazy { application.attributes[RoomManagerKey] }
    val connectionManager by lazy { application.attributes[ConnectionManagerKey] }

    webSocket("/ws/room/{roomId}") {
        val roomId = call.parameters["roomId"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing roomId"))

        val room = roomManager.getRoom(roomId)
        if (room == null) {
            application.log.warn("[GameSocket] Room not found: roomId=$roomId")
            return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room not found"))
        }

        // Reuse existing playerId if provided, otherwise generate new
        val requestedPlayerId = call.parameters["playerId"]
        val isReturningPlayer = !requestedPlayerId.isNullOrBlank()
        val playerId = if (isReturningPlayer) requestedPlayerId else UUID.randomUUID().toString()

        application.log.info("[GameSocket] Connection opened: roomId=$roomId, requestedPlayerId=$requestedPlayerId, assignedPlayerId=$playerId, isReturningPlayer=$isReturningPlayer")

        var playerName = "Player-${playerId.take(4)}"
        var isJoined = false

        // Send welcome message
        application.log.info("[GameSocket] Sending Welcome: playerId=$playerId")
        sendSerialized<ServerMessage>(ServerMessage.Welcome(playerId))

        try {
            while (true) {
                val message = receiveDeserialized<ClientMessage>()
                application.log.info("Received message from player $playerId: $message")
                handleClientMessage(
                    message = message,
                    playerId = playerId,
                    playerName = playerName,
                    room = room,
                    roomId = roomId,
                    connectionManager = connectionManager,
                    session = this,
                    isJoined = isJoined,
                    onJoined = { name ->
                        playerName = name
                        isJoined = true
                    },
                    log = application.log,
                )
            }
        } catch (_: ClosedReceiveChannelException) {
            application.log.info("[GameSocket] Client disconnected (channel closed): playerId=$playerId, roomId=$roomId")
        } catch (e: Exception) {
            application.log.error("[GameSocket] Error: playerId=$playerId, roomId=$roomId, error=${e.message}")
            sendSerialized<ServerMessage>(ServerMessage.Error("INTERNAL_ERROR", e.message ?: "Unknown error"))
        } finally {
            // Cleanup on disconnect
            application.log.info("[GameSocket] Cleaning up: playerId=$playerId, roomId=$roomId, wasJoined=$isJoined")
            if (isJoined) {
                connectionManager.removeConnection(roomId, playerId)
                connectionManager.broadcast(roomId, ServerMessage.PlayerDisconnected(playerId))
                application.log.info("[GameSocket] Player disconnected broadcast sent: playerId=$playerId")

                // NOTE: We do NOT stand the player up here anymore.
                // This allows them to reconnect and resume their seat.
                // The room logic should eventually timeout inactive players (future improvement).
            }
        }
    }
}

private suspend fun handleClientMessage(
    message: ClientMessage,
    playerId: String,
    playerName: String,
    room: ServerRoom,
    roomId: String,
    connectionManager: ConnectionManager,
    session: WebSocketServerSession,
    isJoined: Boolean,
    onJoined: (String) -> Unit,
    log: org.slf4j.Logger,
) {
    when (message) {
        is ClientMessage.JoinRoom -> {
            log.info("[GameSocket] JoinRoom: playerId=$playerId, requestedName=${message.playerName}, alreadyJoined=$isJoined, roomId=$roomId")
            if (isJoined) {
                log.warn("[GameSocket] JoinRoom rejected - already joined: playerId=$playerId")
                session.sendSerialized<ServerMessage>(ServerMessage.Error("ALREADY_JOINED", "Already joined room"))
                return
            }

            val connection = PlayerConnection(playerId, message.playerName, session)
            connectionManager.addConnection(roomId, connection)
            onJoined(message.playerName)
            log.info("[GameSocket] JoinRoom success: playerId=$playerId, playerName=${message.playerName}")

            // Send room info and current state
            session.sendSerialized<ServerMessage>(ServerMessage.RoomJoined(room.getRoomInfo()))
            room.broadcastVisibleGameState()
            // session.sendSerialized<ServerMessage>(ServerMessage.GameStateUpdate(room.getGameState()))

            // If it's this player's turn, send the action request
            room.getActionRequest()?.let { actionRequest ->
                if (actionRequest.playerId == playerId) {
                    log.info("[GameSocket] Sending pending action request to rejoined player: playerId=$playerId")
                    session.sendSerialized<ServerMessage>(ServerMessage.ActionRequired(actionRequest))
                }
            }

            room.getShowdownRequest()?.let { showdownRequest ->
                if (showdownRequest.playerId == playerId) {
                    log.info("[GameSocket] Sending pending showdown request to rejoined player: playerId=$playerId")
                    session.sendSerialized<ServerMessage>(ServerMessage.ShowdownRequired(showdownRequest))
                }
            }

            // Notify others
            connectionManager.broadcastExcept(
                roomId,
                playerId,
                ServerMessage.PlayerConnected(playerId, message.playerName),
            )
        }

        is ClientMessage.LeaveRoom -> {
            session.close(CloseReason(CloseReason.Codes.NORMAL, "Player left"))
        }

        is ClientMessage.TakeSeat -> {
            if (!isJoined) {
                session.sendSerialized<ServerMessage>(ServerMessage.Error("NOT_JOINED", "Must join room first"))
                return
            }

            val result = room.seatPlayer(playerId, playerName, message.seatNumber, message.buyIn)
            result.fold(
                onSuccess = {
                    session.sendSerialized<ServerMessage>(ServerMessage.GameStateUpdate(room.getGameState()))
                    // Try to start hand if enough players
                    room.startHandIfReady()
                },
                onFailure = { e ->
                    session.sendSerialized<ServerMessage>(ServerMessage.Error("SEAT_ERROR", e.message ?: "Cannot take seat"))
                },
            )
        }

        is ClientMessage.LeaveSeat -> {
            if (!isJoined) {
                session.sendSerialized<ServerMessage>(ServerMessage.Error("NOT_JOINED", "Must join room first"))
                return
            }

            val result = room.standPlayer(playerId)
            result.fold(
                onSuccess = { _ ->
                    session.sendSerialized<ServerMessage>(ServerMessage.GameStateUpdate(room.getGameState()))
                },
                onFailure = { e ->
                    session.sendSerialized<ServerMessage>(ServerMessage.Error("STAND_ERROR", e.message ?: "Cannot leave seat"))
                },
            )
        }

        is ClientMessage.PerformAction -> {
            if (!isJoined) {
                session.sendSerialized<ServerMessage>(ServerMessage.Error("NOT_JOINED", "Must join room first"))
                return
            }

            val result = room.performAction(playerId, message.action)
            result.fold(
                onSuccess = {
                    // State updates are broadcast via game events
                },
                onFailure = { e ->
                    session.sendSerialized<ServerMessage>(ServerMessage.Error("ACTION_ERROR", e.message ?: "Invalid action"))
                },
            )
        }

        is ClientMessage.SendChat -> {
            if (!isJoined) {
                session.sendSerialized<ServerMessage>(ServerMessage.Error("NOT_JOINED", "Must join room first"))
                return
            }

            connectionManager.broadcast(
                roomId,
                ServerMessage.GameEventOccurred(GameEvent.ChatMessage(playerId, message.message)),
            )
        }
    }
}
