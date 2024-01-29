package com.poker.client.desktop.presentation

import com.poker.common.data.remote.dto.PlayerDto

data class PokerState(
    val players: List<PlayerDto> = emptyList(),
)
