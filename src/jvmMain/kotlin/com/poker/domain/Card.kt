package com.poker.domain

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val rank: CardRank = CardRank.Ace,
    val suit: CardSuit = CardSuit.Clubs,
) : Comparable<Card> {
    override fun compareTo(other: Card) = when {
        other == this -> 0
        other.rank > this.rank -> 1
        else -> -1
    }

    override fun toString(): String {
        val rank = when(rank) {
            CardRank.Ace -> " A"
            in CardRank.Two..CardRank.Ten -> " ${rank.value}"
            CardRank.Jack -> " J"
            CardRank.Queen -> " Q"
            CardRank.King -> " K"
            else -> throw IllegalStateException()
        }
        val suit = when(suit) {
            CardSuit.Clubs -> "C"
            CardSuit.Diamonds -> "D"
            CardSuit.Hearts -> "H"
            CardSuit.Spades -> "S"
        }
        return "$rank$suit"
    }
}
