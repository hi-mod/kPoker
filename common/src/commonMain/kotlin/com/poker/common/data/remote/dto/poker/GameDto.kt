package com.poker.common.data.remote.dto.poker

import java.util.Date
import kotlinx.serialization.Serializable
@Serializable
data class GameDto(
    val gameState: String,
    val name: String,
    val description: String,
    val inProgress: Boolean,
    val gameType: GameTypeDto,
    val tableNumber: UInt,
    val level: LevelDto,
    val id: String,
    @Serializable(with = DateSerializer::class)
    val started: Date,
    val buttonPosition: Int,
    val players: List<PlayerDto>,
    val handNumber: Long,
    val activePlayer: PlayerDto? = null,
    val board: List<CardDto>,
    val smallestChipSizeInPlay: Double,
    val pot: Double,
    val viewers: List<String>,
    val maxPlayers: Short,
    val minPlayers: Short,
)
