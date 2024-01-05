package com.poker.common.domain

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

    companion object {
        val AceOfClubs: Card
            get() = Card(CardRank.Ace, CardSuit.Clubs)

        val TwoOfClubs: Card
            get() = Card(CardRank.Two, CardSuit.Clubs)

        val ThreeOfClubs: Card
            get() = Card(CardRank.Three, CardSuit.Clubs)

        val FourOfClubs: Card
            get() = Card(CardRank.Four, CardSuit.Clubs)

        val FiveOfClubs: Card
            get() = Card(CardRank.Five, CardSuit.Clubs)

        val SixOfClubs: Card
            get() = Card(CardRank.Six, CardSuit.Clubs)

        val SevenOfClubs: Card
            get() = Card(CardRank.Seven, CardSuit.Clubs)

        val EightOfClubs: Card
            get() = Card(CardRank.Eight, CardSuit.Clubs)

        val NineOfClubs: Card
            get() = Card(CardRank.Nine, CardSuit.Clubs)

        val TenOfClubs: Card
            get() = Card(CardRank.Ten, CardSuit.Clubs)

        val JackOfClubs: Card
            get() = Card(CardRank.Jack, CardSuit.Clubs)

        val QueenOfClubs: Card
            get() = Card(CardRank.Queen, CardSuit.Clubs)

        val KingOfClubs: Card
            get() = Card(CardRank.King, CardSuit.Clubs)

        val AceOfDiamonds: Card
            get() = Card(CardRank.Ace, CardSuit.Diamonds)

        val TwoOfDiamonds: Card
            get() = Card(CardRank.Two, CardSuit.Diamonds)

        val ThreeOfDiamonds: Card
            get() = Card(CardRank.Three, CardSuit.Diamonds)

        val FourOfDiamonds: Card
            get() = Card(CardRank.Four, CardSuit.Diamonds)

        val FiveOfDiamonds: Card
            get() = Card(CardRank.Five, CardSuit.Diamonds)

        val SixOfDiamonds: Card
            get() = Card(CardRank.Six, CardSuit.Diamonds)

        val SevenOfDiamonds: Card
            get() = Card(CardRank.Seven, CardSuit.Diamonds)

        val EightOfDiamonds: Card
            get() = Card(CardRank.Eight, CardSuit.Diamonds)

        val NineOfDiamonds: Card
            get() = Card(CardRank.Nine, CardSuit.Diamonds)

        val TenOfDiamonds: Card
            get() = Card(CardRank.Ten, CardSuit.Diamonds)

        val JackOfDiamonds: Card
            get() = Card(CardRank.Jack, CardSuit.Diamonds)

        val QueenOfDiamonds: Card
            get() = Card(CardRank.Queen, CardSuit.Diamonds)

        val KingOfDiamonds: Card
            get() = Card(CardRank.King, CardSuit.Diamonds)


        val AceOfHearts: Card
            get() = Card(CardRank.Ace, CardSuit.Hearts)

        val TwoOfHearts: Card
            get() = Card(CardRank.Two, CardSuit.Hearts)

        val ThreeOfHearts: Card
            get() = Card(CardRank.Three, CardSuit.Hearts)

        val FourOfHearts: Card
            get() = Card(CardRank.Four, CardSuit.Hearts)

        val FiveOfHearts: Card
            get() = Card(CardRank.Five, CardSuit.Hearts)

        val SixOfHearts: Card
            get() = Card(CardRank.Six, CardSuit.Hearts)

        val SevenOfHearts: Card
            get() = Card(CardRank.Seven, CardSuit.Hearts)

        val EightOfHearts: Card
            get() = Card(CardRank.Eight, CardSuit.Hearts)

        val NineOfHearts: Card
            get() = Card(CardRank.Nine, CardSuit.Hearts)

        val TenOfHearts: Card
            get() = Card(CardRank.Ten, CardSuit.Hearts)

        val JackOfHearts: Card
            get() = Card(CardRank.Jack, CardSuit.Hearts)

        val QueenOfHearts: Card
            get() = Card(CardRank.Queen, CardSuit.Hearts)

        val KingOfHearts: Card
            get() = Card(CardRank.King, CardSuit.Hearts)

        val AceOfSpades: Card
            get() = Card(CardRank.Ace, CardSuit.Spades)

        val TwoOfSpades: Card
            get() = Card(CardRank.Two, CardSuit.Spades)

        val ThreeOfSpades: Card
            get() = Card(CardRank.Three, CardSuit.Spades)

        val FourOfSpades: Card
            get() = Card(CardRank.Four, CardSuit.Spades)

        val FiveOfSpades: Card
            get() = Card(CardRank.Five, CardSuit.Spades)

        val SixOfSpades: Card
            get() = Card(CardRank.Six, CardSuit.Spades)

        val SevenOfSpades: Card
            get() = Card(CardRank.Seven, CardSuit.Spades)

        val EightOfSpades: Card
            get() = Card(CardRank.Eight, CardSuit.Spades)

        val NineOfSpades: Card
            get() = Card(CardRank.Nine, CardSuit.Spades)

        val TenOfSpades: Card
            get() = Card(CardRank.Ten, CardSuit.Spades)

        val JackOfSpades: Card
            get() = Card(CardRank.Jack, CardSuit.Spades)

        val QueenOfSpades: Card
            get() = Card(CardRank.Queen, CardSuit.Spades)

        val KingOfSpades: Card
            get() = Card(CardRank.King, CardSuit.Spades)
    }
}
