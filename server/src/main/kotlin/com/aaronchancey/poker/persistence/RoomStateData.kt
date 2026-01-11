package com.aaronchancey.poker.persistence

import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import kotlinx.serialization.Serializable

@Serializable
data class RoomStateData(
    val roomId: String,
    val roomName: String,
    val maxPlayers: Int,
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val minBuyIn: ChipAmount,
    val maxBuyIn: ChipAmount,
    val gameState: GameState,
)
