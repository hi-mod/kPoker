package com.aaronchancey.poker.room

import com.aaronchancey.poker.persistence.FilePersistenceManager
import com.aaronchancey.poker.ws.ConnectionManager
import com.aaronchancey.poker.ws.PlayerConnection
import io.ktor.server.websocket.WebSocketServerSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class RoomManagerWatcherTest {

    private lateinit var tempDir: File
    private lateinit var persistenceManager: FilePersistenceManager
    private lateinit var connectionManager: ConnectionManager
    private lateinit var roomManager: RoomManager

    @Before
    fun setup() {
        tempDir = Files.createTempDirectory("poker-test").toFile()
        persistenceManager = FilePersistenceManager(tempDir)
        connectionManager = mockk(relaxed = true)
        roomManager = RoomManager(connectionManager, persistenceManager)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test file modification reloads room and checks for connections`() = runTest {
        // 1. Create a room
        val roomId = "test-room"
        val roomName = "Original Name"
        roomManager.createRoom(roomId, roomName)

        // Mock a connection
        val mockSession = mockk<WebSocketServerSession>(relaxed = true)
        val connection = PlayerConnection("player-1", "Player 1", mockSession)
        every { connectionManager.getConnections(roomId) } returns listOf(connection)

        // 2. Modify the file externally
        val file = persistenceManager.getRoomFile(roomId)
        val currentContent = file.readText()
        val newName = "Modified Name"
        val newContent = currentContent.replace(roomName, newName)

        // Wait a bit to ensure timestamp change
        Thread.sleep(100)
        file.writeText(newContent)
        // Manually push timestamp forward to be sure
        file.setLastModified(System.currentTimeMillis() + 5000)

        // 3. Trigger watcher
        roomManager.checkFiles()

        // 4. Verify reload
        val room = roomManager.getRoom(roomId)
        assertNotNull(room)
        assertEquals(newName, room.getRoomInfo().roomName)

        // 5. Verify room manager looked for connections to disconnect them
        verify { connectionManager.getConnections(roomId) }
    }

    @Test
    fun `test file deletion removes room and checks for connections`() = runTest {
        // 1. Create a room
        val roomId = "delete-room"
        roomManager.createRoom(roomId, "To Delete")

        // Mock a connection
        val mockSession = mockk<WebSocketServerSession>(relaxed = true)
        val connection = PlayerConnection("player-1", "Player 1", mockSession)
        every { connectionManager.getConnections(roomId) } returns listOf(connection)

        // 2. Delete the file
        val file = persistenceManager.getRoomFile(roomId)
        assertTrue(file.delete())

        // 3. Trigger watcher
        roomManager.checkFiles()

        // 4. Verify removal
        assertFalse(roomManager.roomExists(roomId))

        // 5. Verify room manager looked for connections to disconnect them
        verify { connectionManager.getConnections(roomId) }
    }

    @Test
    fun `test new file adds room`() = runTest {
        val roomId = "new-room"
        val file = File(tempDir, "$roomId.json")

        val jsonContent = """
            {
                "roomId": "$roomId",
                "roomName": "New External Room",
                "maxPlayers": 2,
                "smallBlind": 1,
                "bigBlind": 2,
                "minBuyIn": 40,
                "maxBuyIn": 200,
                "gameState": {
                    "table": {
                        "id": "table-$roomId",
                        "name": "Table $roomId",
                        "seats": [
                            { "number": 1, "playerState": null },
                            { "number": 2, "playerState": null }
                        ],
                        "maxPlayers": 2
                    },
                    "phase": "WAITING",
                    "potManager": { "pots": [] },
                    "dealerSeatNumber": 1
                }
            }
        """.trimIndent()

        file.writeText(jsonContent)

        // 3. Trigger watcher
        roomManager.checkFiles()

        val room = roomManager.getRoom(roomId)
        assertNotNull(room)
        assertEquals("New External Room", room.getRoomInfo().roomName)
    }
}
