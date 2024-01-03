package com.poker.server.remote

import com.poker.server.domain.Card
import com.poker.server.domain.GameType
import com.poker.server.domain.Level
import com.poker.server.domain.Player
import kotlinx.serialization.Serializable

@Serializable
data class GameDto(
    val id: String,
    val name: String,
    val description: String,
    val inProgress: Boolean,
    val gameType: GameType,
    val tableNumber: UInt,
    val level: Level,
    val started: String,
    val buttonPosition: Int,
    val players: List<Player>,
    val handNumber: Long,
    val activePlayer: Player?,
    val board: List<Card>,
    val smallestChipSizeInPlay: Double,
    val pot: Double,
    val viewers: List<String>,
    val maxPlayers: Short,
    val minPlayers: Short,
)
