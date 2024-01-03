package com.poker.common.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: String = "",
    val name: String = "",
    val chips: Double = 0.0,
    val hand: List<CardDto>? = listOf(),
    val availablePlayerActions: List<PokerActionDto> = emptyList(),
    val currentWager: Double = 0.0,
    val hasActed: Boolean = false,
    val hasFolded: Boolean = false,
)
