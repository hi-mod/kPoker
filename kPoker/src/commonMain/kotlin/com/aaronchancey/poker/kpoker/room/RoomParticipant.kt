package com.aaronchancey.poker.kpoker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player

sealed class RoomParticipant {
    abstract val player: Player
    abstract val joinedAt: Long

    data class Spectator(
        override val player: Player,
        override val joinedAt: Long = currentTimeMillis(),
    ) : RoomParticipant()

    data class SeatedPlayer(
        override val player: Player,
        val seatNumber: Int,
        val chips: ChipAmount,
        override val joinedAt: Long = currentTimeMillis(),
    ) : RoomParticipant()

    companion object {
        private fun currentTimeMillis(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }
}
