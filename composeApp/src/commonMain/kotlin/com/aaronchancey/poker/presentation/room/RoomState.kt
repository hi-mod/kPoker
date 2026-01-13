package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.shared.message.RoomInfo

data class RoomState(
    val isLoading: Boolean = false,
    val rooms: List<RoomInfo> = emptyList(),
)
