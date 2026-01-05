package com.aaronchancey.poker.kpoker.core

enum class Rank(val value: Int, val symbol: String) {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "T"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A"),
    ;

    companion object {
        fun fromSymbol(symbol: String): Rank = entries.first { it.symbol.equals(symbol, ignoreCase = true) }

        fun fromValue(value: Int): Rank = entries.first { it.value == value }
    }
}
