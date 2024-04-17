package com.poker.client.desktop.presentation

import com.poker.common.data.remote.dto.poker.PlayerDto

data class PokerState(
    val loggedIn: Boolean = false,
    val username: String = "",
    val password: String = "",
    val players: List<PlayerDto> = emptyList(),
    val exitApplication: Boolean = false,
    val showLogin: Boolean = false,
)
