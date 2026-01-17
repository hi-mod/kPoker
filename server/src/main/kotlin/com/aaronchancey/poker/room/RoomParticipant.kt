package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player

/**
 * Represents a participant in a room.
 */
sealed class RoomParticipant {
    abstract val player: Player
    abstract val joinedAt: Long

    data class Spectator(
        override val player: Player,
        override val joinedAt: Long = System.currentTimeMillis(),
    ) : RoomParticipant()

    data class SeatedPlayer(
        override val player: Player,
        val seatNumber: Int,
        val chips: ChipAmount,
        override val joinedAt: Long = System.currentTimeMillis(),
    ) : RoomParticipant()
}
