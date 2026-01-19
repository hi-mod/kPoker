package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.ChipAmount

/**
 * User-initiated actions in a poker room.
 *
 * Note: Room joining is handled automatically by [RoomViewModel] when
 * its uiState is collected, using [RoomParams] passed via construction.
 */
sealed interface RoomIntent {
    data object LeaveRoom : RoomIntent
    data class TakeSeat(val seatNumber: Int, val buyIn: ChipAmount) : RoomIntent
    data object LeaveSeat : RoomIntent
    data class PerformAction(val action: Action) : RoomIntent
    data class SendChat(val message: String) : RoomIntent
    data object Disconnect : RoomIntent
    data object ClearError : RoomIntent
}
