package com.aaronchancey.poker.presentation.lobby

/**
 * Intents for the lobby screen.
 */
sealed interface LobbyIntent {
    /** Join a room from the lobby */
    data class JoinRoom(
        val roomId: String,
        val roomName: String,
        val playerName: String,
    ) : LobbyIntent

    /** Close a room window */
    data class CloseRoom(val roomId: String) : LobbyIntent

    /** Clear the saved room session */
    data object ClearSavedRoom : LobbyIntent

    /** Refresh the room list */
    data object RefreshRooms : LobbyIntent
}
