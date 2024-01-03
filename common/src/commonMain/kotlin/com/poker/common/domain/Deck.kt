package com.poker.common.domain

data class Deck(
    val cards: MutableList<Card> = CardSuit.entries
        .flatMap { s ->
            CardRank.entries.map { r -> Card(r, s) }
        }.toMutableList(),
) {

    fun popCard(): Card {
        if (cards.isEmpty()) throw NoSuchElementException("Deck is empty")
        return cards.removeAt(0)
    }

    fun popCards(count: Int): List<Card> {
        if (cards.size < count) throw IllegalArgumentException("Not enough cards in the deck")
        return List(count) { popCard() }
    }

    fun shuffle() {
        cards.shuffle()
    }
}
