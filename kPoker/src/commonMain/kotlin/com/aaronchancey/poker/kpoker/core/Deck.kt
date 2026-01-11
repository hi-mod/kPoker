package com.aaronchancey.poker.kpoker.core

import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Deck(
    private val cards: MutableList<Card> = Card.all().toMutableList(),
    private var position: Int = 0,
) {
    @Transient
    private val random: Random = Random.Default

    val remaining: Int get() = cards.size - position

    fun shuffle() {
        cards.shuffle(random)
        position = 0
    }

    fun deal(): Card {
        check(remaining > 0) { "No cards remaining in deck" }
        return cards[position++]
    }

    fun deal(count: Int): List<Card> {
        check(remaining >= count) { "Not enough cards remaining: need $count, have $remaining" }
        return (1..count).map { deal() }
    }

    fun burn() {
        check(remaining > 0) { "No cards remaining to burn" }
        position++
    }

    fun reset() {
        position = 0
    }

    fun removeCards(cardsToRemove: List<Card>) {
        cardsToRemove.forEach { card ->
            cards.remove(card)
        }
    }

    companion object {
        fun standard(): Deck = Deck().apply { shuffle() }
    }
}
