package com.aaronchancey.poker.window

/**
 * Request to open a room window.
 * Used to pass room information between the lobby and room windows.
 */
data class RoomWindowRequest(
    val roomId: String,
    val roomName: String,
    val playerName: String,
    val playerId: String,
)
