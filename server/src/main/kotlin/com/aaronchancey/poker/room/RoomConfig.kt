package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.shared.model.GameVariant

/**
 * Configuration for a server room.
 */
data class RoomConfig(
    val roomId: String,
    val roomName: String,
    val maxPlayers: Int = 9,
    val smallBlind: ChipAmount = 1.0,
    val bigBlind: ChipAmount = 2.0,
    val minBuyIn: ChipAmount = 40.0,
    val maxBuyIn: ChipAmount = 200.0,
    val minDenomination: ChipAmount = 0.1,
    val variant: GameVariant = GameVariant.TEXAS_HOLDEM_NL,
    val maxSpectators: Int = 100,
    val allowSpectators: Boolean = true,
    val reservationDurationMs: Long = 60_000,
)
