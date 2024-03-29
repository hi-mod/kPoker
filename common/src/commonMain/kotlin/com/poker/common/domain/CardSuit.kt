package com.poker.common.domain

enum class CardSuit(val value: Int, private val shortName: String) {
    Clubs(0, "C"),
    Diamonds(1, "D"),
    Hearts(2, "H"),
    Spades(3, "S"),
    ;

    override fun toString() = shortName
}
