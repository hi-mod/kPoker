package com.poker.common.domain

import java.util.UUID

data class Game(
    val id: UUID,
    val name: String,
    val description: String,
    val numPlayers: Int,
    val level: Level,
)
