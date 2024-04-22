package com.poker.client.desktop.table.presentation

import com.poker.common.data.remote.dto.poker.PlayerDto
import com.poker.common.domain.Game

data class PokerState(
    val loggedIn: Boolean = false,
    val username: String = "",
    val password: String = "",
    val players: List<PlayerDto> = emptyList(),
    val exitApplication: Boolean = false,
    val showLogin: Boolean = false,
    val availableGames: List<Game> = emptyList(),
)
