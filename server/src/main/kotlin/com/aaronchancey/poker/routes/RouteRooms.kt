package com.aaronchancey.poker.routes

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.persistence.FilePersistenceManager
import com.aaronchancey.poker.room.RoomManager
import com.aaronchancey.poker.shared.model.GameVariant
import com.aaronchancey.poker.ws.ConnectionManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomRequest(
    val roomName: String,
    val maxPlayers: Int = 9,
    val smallBlind: ChipAmount = 1.0,
    val bigBlind: ChipAmount = 2.0,
    val minBuyIn: ChipAmount = 40.0,
    val maxBuyIn: ChipAmount = 200.0,
    val variant: GameVariant = GameVariant.TEXAS_HOLDEM_NL,
)

fun Route.routeRooms() {
    val roomManager = application.attributes.getOrNull(RoomManagerKey) ?: run {
        val connectionManager = application.attributes.getOrNull(ConnectionManagerKey) ?: ConnectionManager()
        val persistenceManager = FilePersistenceManager()
        val manager = RoomManager(connectionManager, persistenceManager)

        application.attributes.put(RoomManagerKey, manager)
        application.attributes.put(ConnectionManagerKey, connectionManager)
        manager
    }

    // Hook into shutdown to save rooms
    application.monitor.subscribe(ApplicationStopping) {
        roomManager.saveAllRooms()
    }

    route("/rooms") {
        get {
            val rooms = roomManager.listRooms().sortedBy { it.roomName }
            call.respond(rooms)
        }

        post {
            val request = call.receive<CreateRoomRequest>()
            val roomId = java.util.UUID.randomUUID().toString().take(8)

            val room = roomManager.createRoom(
                roomId = roomId,
                roomName = request.roomName,
                maxPlayers = request.maxPlayers,
                smallBlind = request.smallBlind,
                bigBlind = request.bigBlind,
                minBuyIn = request.minBuyIn,
                maxBuyIn = request.maxBuyIn,
                variant = request.variant,
            )

            call.respond(HttpStatusCode.Created, room.getRoomInfo())
        }

        get("/{roomId}") {
            val roomId = call.parameters["roomId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing roomId")

            val room = roomManager.getRoom(roomId)
                ?: return@get call.respond(HttpStatusCode.NotFound, "Room not found")

            call.respond(room.getRoomInfo())
        }
    }
}
