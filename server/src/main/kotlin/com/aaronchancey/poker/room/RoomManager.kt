package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.persistence.PersistenceManager
import com.aaronchancey.poker.shared.message.RoomInfo
import com.aaronchancey.poker.ws.ConnectionManager
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RoomManager(
    private val connectionManager: ConnectionManager,
    private val persistenceManager: PersistenceManager,
) {
    private val rooms = ConcurrentHashMap<String, ServerRoom>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val roomFileTimestamps = ConcurrentHashMap<String, Long>()

    init {
        restoreRooms()
        startFileWatcher()
    }

    private fun configureRoom(room: ServerRoom) {
        room.addGameEventListener { event ->
            if (event !is com.aaronchancey.poker.kpoker.events.GameEvent.ChatMessage) {
                saveRoom(room.roomId)
            }
        }
    }

    private fun restoreRooms() {
        val loadedRooms = persistenceManager.loadRooms()
        loadedRooms.forEach { data ->
            val room = ServerRoom(
                roomId = data.roomId,
                roomName = data.roomName,
                maxPlayers = data.maxPlayers,
                smallBlind = data.smallBlind,
                bigBlind = data.bigBlind,
                minBuyIn = data.minBuyIn,
                maxBuyIn = data.maxBuyIn,
                connectionManager = connectionManager,
                initialGameState = data.gameState,
            )
            configureRoom(room)
            rooms[data.roomId] = room

            // Initialize timestamp
            val file = persistenceManager.getRoomFile(data.roomId)
            if (file.exists()) {
                roomFileTimestamps[data.roomId] = file.lastModified()
            }
        }
        println("Restored ${rooms.size} rooms.")
    }

    private fun startFileWatcher() {
        scope.launch {
            while (isActive) {
                try {
                    checkFiles()
                } catch (e: Exception) {
                    println("Error in file watcher: ${e.message}")
                    e.printStackTrace()
                }
                delay(5000) // Check every 5 seconds
            }
        }
    }

    internal suspend fun checkFiles() {
        val files = persistenceManager.getAllRoomFiles()
        val fileRoomIds = files.map { it.nameWithoutExtension }.toSet()

        // 1. Check for new or modified files
        files.forEach { file ->
            val roomId = file.nameWithoutExtension
            val lastMod = file.lastModified()
            val knownTimestamp = roomFileTimestamps[roomId]

            if (!rooms.containsKey(roomId)) {
                // New room found
                println("Found new room file: $roomId")
                loadRoomFromFile(roomId)
                roomFileTimestamps[roomId] = lastMod
            } else if (knownTimestamp != null && lastMod > knownTimestamp) {
                // Modified file found
                println("Detected external modification for room: $roomId")
                loadRoomFromFile(roomId) // This will overwrite current room state and disconnect players
                roomFileTimestamps[roomId] = lastMod
            } else if (knownTimestamp == null) {
                // Existing room but first time we see timestamp (shouldn't happen often if restoreRooms works)
                roomFileTimestamps[roomId] = lastMod
            }
        }

        // 2. Check for deleted files
        // We iterate over a copy of keys to avoid concurrent modification issues
        rooms.keys.toList().forEach { roomId ->
            if (!fileRoomIds.contains(roomId)) {
                println("Room file deleted for: $roomId. Removing room.")

                // Disconnect players
                val connections = connectionManager.getConnections(roomId)
                connections.forEach {
                    try {
                        it.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Room deleted"))
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                rooms.remove(roomId)
                roomFileTimestamps.remove(roomId)
            }
        }
    }

    private suspend fun loadRoomFromFile(roomId: String) {
        val data = persistenceManager.loadRoom(roomId) ?: return

        // Notify/Disconnect existing players if any
        if (rooms.containsKey(roomId)) {
            val connections = connectionManager.getConnections(roomId)
            connections.forEach {
                try {
                    it.session.close(CloseReason(CloseReason.Codes.SERVICE_RESTART, "Room reloaded"))
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        val newRoom = ServerRoom(
            roomId = data.roomId,
            roomName = data.roomName,
            maxPlayers = data.maxPlayers,
            smallBlind = data.smallBlind,
            bigBlind = data.bigBlind,
            minBuyIn = data.minBuyIn,
            maxBuyIn = data.maxBuyIn,
            connectionManager = connectionManager,
            initialGameState = data.gameState,
        )
        configureRoom(newRoom)
        rooms[roomId] = newRoom
    }

    fun saveAllRooms() {
        rooms.values.forEach { room ->
            saveRoom(room.roomId)
        }
    }

    fun saveRoom(roomId: String) {
        val room = rooms[roomId] ?: return
        persistenceManager.saveRoom(room.getRoomStateData())

        // Update timestamp so we don't reload our own save
        val file = persistenceManager.getRoomFile(roomId)
        if (file.exists()) {
            roomFileTimestamps[roomId] = file.lastModified()
        }
    }

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
        configureRoom(room)
        rooms[roomId] = room
        saveRoom(roomId)
        return room
    }

    fun getRoom(roomId: String): ServerRoom? = rooms[roomId]

    fun getRoomOrCreate(
        roomId: String,
        roomName: String = "Room $roomId",
    ): ServerRoom = rooms.getOrPut(roomId) {
        val room = ServerRoom(
            roomId = roomId,
            roomName = roomName,
            connectionManager = connectionManager,
        )
        configureRoom(room)
        // We call saveRoom explicitly to persist it and set timestamp
        // But getOrPut returns before we can call saveRoom if we put logic here.
        // Actually getOrPut takes a lambda to CREATE the value.
        // So we can do:
        // persistenceManager.saveRoom(room.getRoomStateData())
        // BUT we need to set timestamp too.
        // Let's just call saveRoom(roomId) after creation in the caller?
        // No, getRoomOrCreate is atomic-ish.

        // Let's simplify:
        persistenceManager.saveRoom(room.getRoomStateData())
        val file = persistenceManager.getRoomFile(roomId)
        if (file.exists()) {
            roomFileTimestamps[roomId] = file.lastModified()
        }

        room
    }

    fun listRooms(): List<RoomInfo> = rooms.values.map { it.getRoomInfo() }

    fun removeRoom(roomId: String) {
        rooms.remove(roomId)
        persistenceManager.deleteRoom(roomId)
        roomFileTimestamps.remove(roomId)
    }

    fun roomExists(roomId: String): Boolean = rooms.containsKey(roomId)
}
