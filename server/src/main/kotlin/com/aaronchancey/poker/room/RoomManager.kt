package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.persistence.PersistenceManager
import com.aaronchancey.poker.shared.message.RoomInfo
import com.aaronchancey.poker.shared.model.GameVariant
import com.aaronchancey.poker.ws.ConnectionManager
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomManager(
    private val connectionManager: ConnectionManager,
    private val persistenceManager: PersistenceManager,
) {
    private val rooms = ConcurrentHashMap<String, ServerRoom>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val roomFileTimestamps = ConcurrentHashMap<String, Long>()

    private val managementMutex = Mutex()

    init {
        restoreRooms()
        startRoomMonitor()
        startFileWatcher()
    }

    private fun startRoomMonitor() {
        scope.launch {
            while (isActive) {
                try {
                    managementMutex.withLock {
                        GameVariant.entries.forEach { variant ->
                            manageVariantCapacity(variant)
                        }
                    }
                } catch (e: Exception) {
                    println("Error in room monitor: ${e.message}")
                    e.printStackTrace()
                }
                delay(10.seconds)
            }
        }
    }

    private fun manageVariantCapacity(variant: GameVariant) {
        val variantRooms = rooms.values.filter { it.variant == variant }

        // 1. Ensure Capacity: If no rooms or all are full, create one
        // We consider a room "full" if it has reached maxPlayers.
        // We want at least one room where a player can sit.
        val availableRooms = variantRooms.filter { it.getRoomInfo().playerCount < it.maxPlayers }
        if (availableRooms.isEmpty()) {
            createAutoRoom(variant)
        }

        // 2. Garbage Collection: If > 1 empty room, remove extras
        // An "empty" room has 0 players.
        val emptyRooms = variantRooms.filter { it.getRoomInfo().playerCount == 0 }
        if (emptyRooms.size > 1) {
            // Keep the one created most recently (or just arbitrary), remove others.
            // rooms is a map, order isn't guaranteed, but we can just drop(1).
            // Let's sort by roomId to be deterministic
            val roomsToRemove = emptyRooms.sortedBy { it.roomId }.drop(1)
            roomsToRemove.forEach { room ->
                println("Garbage collecting empty room: ${room.roomName} (${room.roomId})")
                removeRoom(room.roomId)
            }
        }
    }

    private fun createAutoRoom(variant: GameVariant) {
        val uuid = UUID.randomUUID().toString()
        val shortId = uuid.take(8)
        val roomId = "${variant.name.lowercase().replace("_", "-")}-$shortId"
        val roomName = "${variant.displayName} $shortId"

        println("Auto-creating room: $roomName")
        createRoom(
            roomId = roomId,
            roomName = roomName,
            variant = variant,
            // Use defaults for blinds/buyins defined in createRoom
        )
    }

    private fun configureRoom(room: ServerRoom) {
        room.addGameEventListener { event ->
            if (event !is GameEvent.ChatMessage) {
                saveRoom(room.roomId)
            }
        }
    }

    private fun constructRoom(data: com.aaronchancey.poker.persistence.RoomStateData): ServerRoom {
        val room = ServerRoom(
            roomId = data.roomId,
            roomName = data.roomName,
            minDenomination = data.minDenomination,
            maxPlayers = data.maxPlayers,
            smallBlind = data.smallBlind,
            bigBlind = data.bigBlind,
            minBuyIn = data.minBuyIn,
            maxBuyIn = data.maxBuyIn,
            variant = data.variant,
            connectionManager = connectionManager,
            initialGameState = data.gameState,
        )
        configureRoom(room)
        return room
    }

    private fun restoreRooms() {
        val loadedRooms = persistenceManager.loadRooms()
        loadedRooms.forEach { data ->
            val room = constructRoom(data)
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
                    } catch (_: Exception) {
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
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }

        val newRoom = constructRoom(data)
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
        smallBlind: ChipAmount = 1.0,
        bigBlind: ChipAmount = 2.0,
        minBuyIn: ChipAmount = 40.0,
        maxBuyIn: ChipAmount = 200.0,
        minDenomination: ChipAmount = 1.0,
        variant: GameVariant = GameVariant.TEXAS_HOLDEM_NL,
    ): ServerRoom {
        val room = ServerRoom(
            roomId = roomId,
            roomName = roomName,
            minDenomination = minDenomination,
            maxPlayers = maxPlayers,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            minBuyIn = minBuyIn,
            maxBuyIn = maxBuyIn,
            variant = variant,
            connectionManager = connectionManager,
        )
        configureRoom(room)
        rooms[roomId] = room
        saveRoom(roomId)
        return room
    }

    fun getRoom(roomId: String): ServerRoom? = rooms[roomId]

    fun listRooms(): List<RoomInfo> = rooms.values.map { it.getRoomInfo() }

    fun removeRoom(roomId: String) {
        rooms.remove(roomId)
        persistenceManager.deleteRoom(roomId)
        roomFileTimestamps.remove(roomId)
    }

    fun roomExists(roomId: String): Boolean = rooms.containsKey(roomId)
}
