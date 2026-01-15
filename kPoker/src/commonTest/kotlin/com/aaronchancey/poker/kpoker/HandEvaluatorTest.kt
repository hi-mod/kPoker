package com.aaronchancey.poker.kpoker

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.HandRank
import com.aaronchancey.poker.kpoker.evaluation.StandardHandEvaluator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandEvaluatorTest {
    private val evaluator = StandardHandEvaluator()

    private fun cards(vararg cardStrings: String): List<Card> = cardStrings.map { Card.fromString(it) }

    @Test
    fun testRoyalFlush() {
        val hand = cards("As", "Ks", "Qs", "Js", "Ts")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.ROYAL_FLUSH, result.rank)
    }

    @Test
    fun testStraightFlush() {
        val hand = cards("9h", "8h", "7h", "6h", "5h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.STRAIGHT_FLUSH, result.rank)
    }

    @Test
    fun testFourOfAKind() {
        val hand = cards("Ah", "As", "Ad", "Ac", "Kh")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.FOUR_OF_A_KIND, result.rank)
    }

    @Test
    fun testFullHouse() {
        val hand = cards("Kh", "Ks", "Kd", "Qc", "Qh")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.FULL_HOUSE, result.rank)
    }

    @Test
    fun testFlush() {
        val hand = cards("Ah", "Kh", "9h", "5h", "2h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.FLUSH, result.rank)
    }

    @Test
    fun testStraight() {
        val hand = cards("9h", "8s", "7d", "6c", "5h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.STRAIGHT, result.rank)
    }

    @Test
    fun testWheel() {
        val hand = cards("5h", "4s", "3d", "2c", "Ah")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.STRAIGHT, result.rank)
    }

    @Test
    fun testThreeOfAKind() {
        val hand = cards("Qh", "Qs", "Qd", "Kc", "2h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.THREE_OF_A_KIND, result.rank)
    }

    @Test
    fun testTwoPair() {
        val hand = cards("Kh", "Ks", "Qd", "Qc", "2h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.TWO_PAIR, result.rank)
    }

    @Test
    fun testOnePair() {
        val hand = cards("Ah", "As", "Kd", "Qc", "Jh")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.ONE_PAIR, result.rank)
    }

    @Test
    fun testHighCard() {
        val hand = cards("Ah", "Ks", "9d", "5c", "2h")
        val result = evaluator.evaluate(hand)
        assertEquals(HandRank.HIGH_CARD, result.rank)
    }

    @Test
    fun testFindBestHandFromSeven() {
        val cards = cards("As", "Ks", "Qs", "Js", "Ts", "2h", "3d")
        val result = evaluator.findBestHand(cards, 5).first()
        assertEquals(HandRank.ROYAL_FLUSH, result.rank)
    }

    @Test
    fun testHandComparison() {
        val flush = evaluator.evaluate(cards("Ah", "Kh", "9h", "5h", "2h"))
        val straight = evaluator.evaluate(cards("9h", "8s", "7d", "6c", "5h"))
        assertTrue(flush > straight)
    }
}
