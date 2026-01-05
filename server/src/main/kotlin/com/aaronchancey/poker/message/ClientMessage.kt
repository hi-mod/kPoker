package com.aaronchancey.poker.message

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.player.ChipAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    @SerialName("join_room")
    data class JoinRoom(val playerName: String) : ClientMessage()

    @Serializable
    @SerialName("leave_room")
    data object LeaveRoom : ClientMessage()

    @Serializable
    @SerialName("take_seat")
    data class TakeSeat(val seatNumber: Int, val buyIn: ChipAmount) : ClientMessage()

    @Serializable
    @SerialName("leave_seat")
    data object LeaveSeat : ClientMessage()

    @Serializable
    @SerialName("perform_action")
    data class PerformAction(val action: Action) : ClientMessage()

    @Serializable
    @SerialName("send_chat")
    data class SendChat(val message: String) : ClientMessage()
}
