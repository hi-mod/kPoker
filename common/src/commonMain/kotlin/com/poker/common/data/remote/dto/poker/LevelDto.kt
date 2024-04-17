package com.poker.common.data.remote.dto.poker

import kotlinx.serialization.Serializable

@Serializable
data class LevelDto(
    val smallBlind: Double = 0.0,
    val bigBlind: Double = 0.0,
    val ante: Double? = null,
    val duration: Int? = null,
)
