package com.aaronchancey.poker.kpoker

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Deck
import com.aaronchancey.poker.kpoker.core.Rank
import com.aaronchancey.poker.kpoker.core.Suit
import kotlin.test.Test
import kotlin.test.assertEquals

class CardTest {
    @Test
    fun testCardCreation() {
        val card = Card(Rank.ACE, Suit.SPADES)
        assertEquals(Rank.ACE, card.rank)
        assertEquals(Suit.SPADES, card.suit)
        assertEquals("As", card.toString())
    }

    @Test
    fun testCardFromString() {
        val card = Card.fromString("Kh")
        assertEquals(Rank.KING, card.rank)
        assertEquals(Suit.HEARTS, card.suit)
    }

    @Test
    fun testDeckHas52Cards() {
        val deck = Deck()
        assertEquals(52, deck.remaining)
    }

    @Test
    fun testDeckDeal() {
        val deck = Deck()
        deck.shuffle()
        deck.deal()
        assertEquals(51, deck.remaining)
    }

    @Test
    fun testDeckDealMultiple() {
        val deck = Deck()
        deck.shuffle()
        val cards = deck.deal(5)
        assertEquals(5, cards.size)
        assertEquals(47, deck.remaining)
    }
}
