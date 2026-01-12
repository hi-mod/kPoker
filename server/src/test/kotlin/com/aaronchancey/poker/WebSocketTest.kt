package com.aaronchancey.poker

import com.aaronchancey.poker.persistence.FilePersistenceManager
import com.aaronchancey.poker.room.RoomManager
import com.aaronchancey.poker.routes.ConnectionManagerKey
import com.aaronchancey.poker.routes.RoomManagerKey
import com.aaronchancey.poker.shared.message.ClientMessage
import com.aaronchancey.poker.shared.message.ServerMessage
import com.aaronchancey.poker.ws.ConnectionManager
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.testing.testApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class WebSocketTest {

    @Test
    fun testReconnectionReceivesActionRequest() {
        val tempDir = File("build/tmp/test_rooms_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            testApplication {
                val connectionManager = ConnectionManager()
                val persistenceManager = FilePersistenceManager(tempDir)
                val roomManager = RoomManager(connectionManager, persistenceManager)

                application {
                    attributes.put(RoomManagerKey, roomManager)
                    attributes.put(ConnectionManagerKey, connectionManager)
                    module()
                }

                val client = createClient {
                    install(WebSockets) {
                        contentConverter = KotlinxWebsocketSerializationConverter(Json { ignoreUnknownKeys = true })
                    }
                }

                val roomId = "testRoom"
                roomManager.createRoom(roomId, "Test Room")

                val p1Id = "p1"
                val p2Id = "p2"

                // 1. Connect P1 and join/seat
                client.webSocket("/ws/room/$roomId?playerId=$p1Id") {
                    // Receive Welcome
                    val welcome = receiveDeserialized<ServerMessage>()
                    assertIs<ServerMessage.Welcome>(welcome)

                    // Join Room
                    sendSerialized(ClientMessage.JoinRoom("Player 1") as ClientMessage)

                    // Receive Joined & State
                    assertIs<ServerMessage.RoomJoined>(receiveDeserialized<ServerMessage>())
                    assertIs<ServerMessage.GameStateUpdate>(receiveDeserialized<ServerMessage>())

                    // Take Seat
                    sendSerialized(ClientMessage.TakeSeat(1, 100) as ClientMessage)
                    assertIs<ServerMessage.GameStateUpdate>(receiveDeserialized<ServerMessage>())
                }

                // 2. Connect P2 and join/seat to start the game
                client.webSocket("/ws/room/$roomId?playerId=$p2Id") {
                    receiveDeserialized<ServerMessage>() // Welcome
                    sendSerialized(ClientMessage.JoinRoom("Player 2") as ClientMessage)
                    assertIs<ServerMessage.RoomJoined>(receiveDeserialized<ServerMessage>())
                    assertIs<ServerMessage.GameStateUpdate>(receiveDeserialized<ServerMessage>())

                    // Take Seat - this should trigger game start
                    sendSerialized(ClientMessage.TakeSeat(2, 100) as ClientMessage)

                    // We expect at least one state update (seated)
                    receiveDeserialized<ServerMessage>()
                }

                // Wait a bit for game start logic (coroutines) to propagate
                Thread.sleep(1000)

                // 3. Identify who is acting
                val gameState = roomManager.getRoom(roomId)!!.getGameState()
                val currentActor = gameState.currentActor?.player?.id

                assertTrue(currentActor != null, "Game should have started and have a current actor")

                // 4. Reconnect the CURRENT ACTOR
                val targetId = currentActor!!
                println("Reconnecting target actor: $targetId")

                client.webSocket("/ws/room/$roomId?playerId=$targetId") {
                    val welcome = receiveDeserialized<ServerMessage>()
                    assertIs<ServerMessage.Welcome>(welcome)

                    sendSerialized(ClientMessage.JoinRoom("Reconnector") as ClientMessage)

                    assertIs<ServerMessage.RoomJoined>(receiveDeserialized<ServerMessage>())
                    assertIs<ServerMessage.GameStateUpdate>(receiveDeserialized<ServerMessage>())

                    // This is what we are fixing: we expect ActionRequired
                    val actionMsg = receiveDeserialized<ServerMessage>()
                    assertIs<ServerMessage.ActionRequired>(actionMsg)
                    assertEquals(targetId, actionMsg.request.playerId)
                }
            }
        } finally {
            // Cleanup after test application has shut down
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        }
    }
}
