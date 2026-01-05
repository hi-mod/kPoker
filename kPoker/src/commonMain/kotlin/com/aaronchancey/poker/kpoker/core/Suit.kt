package com.aaronchancey.poker.kpoker.core

import kotlinx.serialization.Serializable

@Serializable
enum class Suit(val symbol: String, val unicode: String) {
    CLUBS("c", "♣"),
    DIAMONDS("d", "♦"),
    HEARTS("h", "♥"),
    SPADES("s", "♠"),
    ;

    companion object {
        fun fromSymbol(symbol: String): Suit = entries.first { it.symbol.equals(symbol, ignoreCase = true) }
    }
}
