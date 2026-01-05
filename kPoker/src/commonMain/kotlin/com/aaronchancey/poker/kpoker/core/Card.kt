package com.aaronchancey.poker.kpoker.core

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
) : Comparable<Card> {

    override fun compareTo(other: Card): Int = rank.value.compareTo(other.rank.value)

    override fun toString(): String = "${rank.symbol}${suit.symbol}"

    fun toUnicode(): String = "${rank.symbol}${suit.unicode}"

    companion object {
        fun fromString(str: String): Card {
            require(str.length == 2) { "Card string must be 2 characters (e.g., 'As', 'Kh')" }
            val rank = Rank.fromSymbol(str[0].toString())
            val suit = Suit.fromSymbol(str[1].toString())
            return Card(rank, suit)
        }

        fun all(): List<Card> = Suit.entries.flatMap { suit ->
            Rank.entries.map { rank -> Card(rank, suit) }
        }
    }
}
