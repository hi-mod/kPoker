package com.poker.common.domain

data class Level(
    val smallBlind: Double = 0.0,
    val bigBlind: Double = 0.0,
    val ante: Double? = null,
    val duration: Int? = null,
)
