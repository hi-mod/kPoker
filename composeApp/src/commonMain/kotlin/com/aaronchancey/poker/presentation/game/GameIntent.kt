package com.aaronchancey.poker.presentation.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.ChipAmount

// Intent (User Actions)
sealed interface GameIntent {
    data class JoinRoom(val playerName: String, val roomId: String) : GameIntent
    data object LeaveRoom : GameIntent
    data class TakeSeat(val seatNumber: Int, val buyIn: ChipAmount) : GameIntent
    data object LeaveSeat : GameIntent
    data class PerformAction(val action: Action) : GameIntent
    data class SendChat(val message: String) : GameIntent
    data object Disconnect : GameIntent
    data object ClearError : GameIntent
}
