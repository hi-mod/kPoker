package com.aaronchancey.poker.kpoker

import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.room.Room
import com.aaronchancey.poker.kpoker.room.RoomConfig
import com.aaronchancey.poker.kpoker.room.SeatSelectionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoomTest {
    private fun createRoom(): Room {
        val config = RoomConfig(
            id = "test-room",
            name = "Test Room",
            maxPlayers = 6,
            minBuyIn = 100.0,
            maxBuyIn = 1000.0,
        )
        return Room(config)
    }

    private fun createPlayer(id: String, name: String) = Player(id, name)

    @Test
    fun testJoinAsSpectator() {
        val room = createRoom()
        val player = createPlayer("p1", "Alice")

        val result = room.joinAsSpectator(player)
        assertTrue(result.isSuccess)
        assertEquals(1, room.spectatorCount)
        assertTrue(room.isPlayerInRoom(player.id))
    }

    @Test
    fun testTakeSeat() {
        val room = createRoom()
        val player = createPlayer("p1", "Alice")

        room.joinAsSpectator(player)
        val result = room.takeSeat(player, 1, 500.0, System.currentTimeMillis())

        assertTrue(result is SeatSelectionResult.Success)
        assertEquals(1, room.playerCount)
        assertEquals(0, room.spectatorCount)
        assertTrue(room.isPlayerSeated(player.id))
    }

    @Test
    fun testSeatOccupied() {
        val room = createRoom()
        val player1 = createPlayer("p1", "Alice")
        val player2 = createPlayer("p2", "Bob")

        room.joinAsSpectator(player1)
        room.joinAsSpectator(player2)

        room.takeSeat(player1, 1, 500.0, System.currentTimeMillis())
        val result = room.takeSeat(player2, 1, 500.0, System.currentTimeMillis())

        assertTrue(result is SeatSelectionResult.SeatOccupied)
    }

    @Test
    fun testStandUp() {
        val room = createRoom()
        val player = createPlayer("p1", "Alice")

        room.joinAsSpectator(player)
        room.takeSeat(player, 1, 500.0, System.currentTimeMillis())

        val result = room.standUp(player.id)
        assertTrue(result.isSuccess)
        assertEquals(500.0, result.getOrNull())
        assertFalse(room.isPlayerSeated(player.id))
        assertEquals(1, room.spectatorCount) // Became spectator
    }

    @Test
    fun testSeatReservation() {
        val room = createRoom()
        val player = createPlayer("p1", "Alice")
        val currentTime = System.currentTimeMillis()

        room.joinAsSpectator(player)
        val result = room.reserveSeat(player.id, 3, currentTime)

        assertTrue(result is SeatSelectionResult.Success)
    }

    @Test
    fun testGetAvailableSeats() {
        val room = createRoom()
        val player1 = createPlayer("p1", "Alice")
        val currentTime = System.currentTimeMillis()

        room.joinAsSpectator(player1)
        room.takeSeat(player1, 1, 500.0, currentTime)

        val available = room.getAvailableSeats(currentTime)
        assertEquals(5, available.size)
        assertFalse(1 in available)
    }

    @Test
    fun testInsufficientBuyIn() {
        val room = createRoom()
        val player = createPlayer("p1", "Alice")

        room.joinAsSpectator(player)
        val result = room.takeSeat(player, 1, 50.0, System.currentTimeMillis())

        assertTrue(result is SeatSelectionResult.InsufficientBuyIn)
    }
}
