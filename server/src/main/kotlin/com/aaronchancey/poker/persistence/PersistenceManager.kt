package com.aaronchancey.poker.persistence

import java.io.File
import kotlinx.serialization.json.Json

interface PersistenceManager {
    fun saveRoom(roomData: RoomStateData)
    fun loadRooms(): List<RoomStateData>
    fun loadRoom(roomId: String): RoomStateData?
    fun deleteRoom(roomId: String)
    fun getRoomFile(roomId: String): File
    fun getAllRoomFiles(): List<File>
}

class FilePersistenceManager(
    private val rootDir: File = File("data/rooms"),
) : PersistenceManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    init {
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }
    }

    override fun saveRoom(roomData: RoomStateData) {
        val file = getRoomFile(roomData.roomId)
        val content = json.encodeToString(RoomStateData.serializer(), roomData)
        file.writeText(content)
    }

    override fun loadRooms(): List<RoomStateData> {
        if (!rootDir.exists()) return emptyList()

        return getAllRoomFiles()
            .mapNotNull { file ->
                try {
                    val content = file.readText()
                    json.decodeFromString(RoomStateData.serializer(), content)
                } catch (e: Exception) {
                    println("Failed to load room from ${file.name}: ${e.message}")
                    null
                }
            }
    }

    override fun loadRoom(roomId: String): RoomStateData? {
        val file = getRoomFile(roomId)
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            json.decodeFromString(RoomStateData.serializer(), content)
        } catch (e: Exception) {
            println("Failed to load room $roomId: ${e.message}")
            null
        }
    }

    override fun deleteRoom(roomId: String) {
        val file = getRoomFile(roomId)
        if (file.exists()) {
            file.delete()
        }
    }

    override fun getRoomFile(roomId: String): File = File(rootDir, "$roomId.json")

    override fun getAllRoomFiles(): List<File> {
        if (!rootDir.exists()) return emptyList()
        return rootDir.listFiles { _, name -> name.endsWith(".json") }?.toList() ?: emptyList()
    }
}
