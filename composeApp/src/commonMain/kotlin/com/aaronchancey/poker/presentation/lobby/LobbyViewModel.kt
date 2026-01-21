package com.aaronchancey.poker.presentation.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.domain.onSuccess
import com.aaronchancey.poker.network.RoomClient
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.window.RoomWindowRequest
import com.russhwolf.settings.Settings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the lobby screen.
 *
 * Manages:
 * - Room list fetching
 * - Saved session restoration on app launch
 * - Room window opening requests
 */
@OptIn(ExperimentalUuidApi::class)
class LobbyViewModel(
    private val settings: Settings,
    private val roomClient: RoomClient,
) : ViewModel() {

    private val _state = MutableStateFlow(LobbyState())
    val state = _state
        .asStateFlow()
        .onStart { fetchRooms() }
        .onStart { checkSavedRoom() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LobbyState())

    fun onIntent(intent: LobbyIntent) = when (intent) {
        is LobbyIntent.JoinRoom -> handleJoinRoom(intent)
        is LobbyIntent.CloseRoom -> handleCloseRoom(intent.roomId)
        is LobbyIntent.ClearSavedRoom -> clearSavedRoom()
        is LobbyIntent.RefreshRooms -> refreshRooms()
    }

    private fun fetchRooms() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        roomClient.getRooms()
            .onSuccess { rooms ->
                _state.update { it.copy(rooms = rooms) }
            }
        _state.update { it.copy(isLoading = false) }
    }

    private fun refreshRooms() {
        fetchRooms()
    }

    /**
     * Checks if there's a saved room session and opens it automatically.
     * Called on app launch via onStart.
     */
    private fun checkSavedRoom() {
        val savedRoomId = settings.getStringOrNull(RoomViewModel.KEY_CURRENT_ROOM_ID)
        println("[LobbyViewModel] checkSavedRoom: savedRoomId=$savedRoomId")

        if (savedRoomId == null) {
            println("[LobbyViewModel] checkSavedRoom: No saved room found, skipping auto-rejoin")
            return
        }

        val savedRoomName = settings.getString(RoomViewModel.KEY_CURRENT_ROOM_NAME, "")
        val savedPlayerName = settings.getString(RoomViewModel.KEY_PLAYER_NAME, "")
        val existingPlayerId = settings.getStringOrNull(RoomViewModel.KEY_PLAYER_ID)
        val playerId = existingPlayerId ?: Uuid.generateV4().toString()

        println("[LobbyViewModel] checkSavedRoom: Found saved session - roomName=$savedRoomName, playerName=$savedPlayerName, existingPlayerId=$existingPlayerId, usingPlayerId=$playerId")

        val request = RoomWindowRequest(
            roomId = savedRoomId,
            roomName = savedRoomName,
            playerName = savedPlayerName,
            playerId = playerId,
        )
        _state.update { it.copy(openRooms = it.openRooms + request) }
        println("[LobbyViewModel] checkSavedRoom: Added request to openRooms")
    }

    /**
     * Handles joining a room from the lobby UI.
     * Adds the room to openRooms state, which the platform layer renders as a window.
     */
    private fun handleJoinRoom(intent: LobbyIntent.JoinRoom) {
        println("[LobbyViewModel] handleJoinRoom: roomId=${intent.roomId}, roomName=${intent.roomName}, playerName=${intent.playerName}")

        val existingPlayerId = settings.getStringOrNull(RoomViewModel.KEY_PLAYER_ID)
        val playerId = existingPlayerId ?: Uuid.generateV4().toString()

        println("[LobbyViewModel] handleJoinRoom: existingPlayerId=$existingPlayerId, usingPlayerId=$playerId")

        // Save player name for convenience
        settings.putString(RoomViewModel.KEY_PLAYER_NAME, intent.playerName)

        val request = RoomWindowRequest(
            roomId = intent.roomId,
            roomName = intent.roomName,
            playerName = intent.playerName,
            playerId = playerId,
        )
        _state.update { it.copy(openRooms = it.openRooms + request) }
        println("[LobbyViewModel] handleJoinRoom: Added request to openRooms, count=${_state.value.openRooms.size}")
    }

    /**
     * Handles closing a room window.
     * Removes the room from openRooms state.
     */
    private fun handleCloseRoom(roomId: String) {
        println("[LobbyViewModel] handleCloseRoom: roomId=$roomId, openRooms before=${_state.value.openRooms.map { it.roomId }}")
        settings.remove(RoomViewModel.KEY_CURRENT_ROOM_ID)
        settings.remove(RoomViewModel.KEY_CURRENT_ROOM_NAME)
        _state.update { state ->
            state.copy(openRooms = state.openRooms.filterNot { it.roomId == roomId }.toSet())
        }
        println("[LobbyViewModel] handleCloseRoom: Removed room, openRooms after=${_state.value.openRooms.map { it.roomId }}")
    }

    /**
     * Clears the saved room session.
     */
    private fun clearSavedRoom() {
        settings.remove(RoomViewModel.KEY_CURRENT_ROOM_ID)
        settings.remove(RoomViewModel.KEY_CURRENT_ROOM_NAME)
    }
}
