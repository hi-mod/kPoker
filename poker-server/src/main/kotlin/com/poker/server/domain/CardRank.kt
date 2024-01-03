package com.poker.server.domain

import kotlinx.serialization.Serializable

@Serializable
enum class CardRank(val value: Int, val shortName: String, val pluralName: String, val cardName: Char) {
    Two(2, "Deuce", "Deuces", '2'),
    Three(3, "Three", "Threes", '3'),
    Four(4, "Four", "Fours", '4'),
    Five(5, "Five", "Fives", '5'),
    Six(6, "Six", "Sixes", '6'),
    Seven(7, "Seven", "Sevens", '7'),
    Eight(8, "Eight", "Eights", '8'),
    Nine(9, "Nine", "Nines", '9'),
    Ten(10, "Ten", "Tens", 'T'),
    Jack(11, "Jack", "Jacks", 'J'),
    Queen(12, "Queen", "Queens", 'Q'),
    King(13, "King", "Kings", 'K'),
    Ace(14, "Ace", "Aces", 'A'),
    ;

    override fun toString() = cardName.toString()
}
