package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.message.RoomInfo
import com.aaronchancey.poker.ws.ConnectionManager
import java.util.concurrent.ConcurrentHashMap

class RoomManager(
    private val connectionManager: ConnectionManager,
) {
    private val rooms = ConcurrentHashMap<String, ServerRoom>()

    fun createRoom(
        roomId: String,
        roomName: String,
        maxPlayers: Int = 9,
        smallBlind: ChipAmount = 1,
        bigBlind: ChipAmount = 2,
        minBuyIn: ChipAmount = 40,
        maxBuyIn: ChipAmount = 200,
    ): ServerRoom {
        val room = ServerRoom(
            roomId = roomId,
            roomName = roomName,
            maxPlayers = maxPlayers,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            minBuyIn = minBuyIn,
            maxBuyIn = maxBuyIn,
            connectionManager = connectionManager,
        )
        rooms[roomId] = room
        return room
    }

    fun getRoom(roomId: String): ServerRoom? = rooms[roomId]

    fun getRoomOrCreate(
        roomId: String,
        roomName: String = "Room $roomId",
    ): ServerRoom = rooms.getOrPut(roomId) {
        ServerRoom(
            roomId = roomId,
            roomName = roomName,
            connectionManager = connectionManager,
        )
    }

    fun listRooms(): List<RoomInfo> = rooms.values.map { it.getRoomInfo() }

    fun removeRoom(roomId: String) {
        rooms.remove(roomId)
    }

    fun roomExists(roomId: String): Boolean = rooms.containsKey(roomId)
}
