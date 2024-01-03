package com.poker.common.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CardDto(
    val rank: CardRankDto = CardRankDto.Ace,
    val suit: CardSuitDto = CardSuitDto.Clubs,
) : Comparable<CardDto> {
    override fun compareTo(other: CardDto) = when {
        other == this -> 0
        other.rank > this.rank -> 1
        else -> -1
    }

    override fun toString(): String {
        val rank = when(rank) {
            CardRankDto.Ace -> " A"
            in CardRankDto.Two..CardRankDto.Ten -> " ${rank.value}"
            CardRankDto.Jack -> " J"
            CardRankDto.Queen -> " Q"
            CardRankDto.King -> " K"
            else -> throw IllegalStateException()
        }
        val suit = when(suit) {
            CardSuitDto.Clubs -> "C"
            CardSuitDto.Diamonds -> "D"
            CardSuitDto.Hearts -> "H"
            CardSuitDto.Spades -> "S"
        }
        return "$rank$suit"
    }
}
