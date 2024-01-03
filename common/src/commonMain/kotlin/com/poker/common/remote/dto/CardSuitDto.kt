package com.poker.common.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class CardSuitDto(val value: Int, private val shortName: String) {
    Clubs(0, "C"),
    Diamonds(1, "D"),
    Hearts(2, "H"),
    Spades(3, "S"),
    ;

    override fun toString() = shortName
}
