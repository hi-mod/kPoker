package com.aaronchancey.poker.presentation.lobby

import com.aaronchancey.poker.shared.message.RoomInfo
import com.aaronchancey.poker.window.RoomWindowRequest

data class LobbyState(
    val isLoading: Boolean = false,
    val rooms: List<RoomInfo> = emptyList(),
    /** Currently open room windows - managed by ViewModel, rendered by platform layer */
    val openRooms: Set<RoomWindowRequest> = emptySet(),
)
