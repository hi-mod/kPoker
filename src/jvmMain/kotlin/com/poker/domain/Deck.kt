package com.poker.domain

import kotlinx.serialization.Serializable

@Serializable
data class Deck(
    val cards: List<Card> = CardRank.entries.flatMap { rank ->
        CardSuit.entries.map { suit ->
            Card(rank, suit)
        }
    },
) : Iterable<Card> {
    override fun iterator(): Iterator<Card> = DeckIterator(cards)
    fun shuffle() = Deck(cards.shuffled())
}

private class DeckIterator(val cards: List<Card>) : Iterator<Card> {

    private var startIndex = 0

    override fun hasNext(): Boolean = cards.isNotEmpty()

    override fun next(): Card {
        if(!hasNext()) throw NoSuchElementException()
        return cards[startIndex++]
    }
}
