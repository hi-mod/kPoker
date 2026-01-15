package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Rank
import com.aaronchancey.poker.kpoker.core.Suit
import com.aaronchancey.poker.kpoker.game.GameVariant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OmahaHiLoTest {

    private val evaluator = HandEvaluatorFactory.getEvaluator(GameVariant.OMAHA_HI_LO)

    @Test
    fun `no low hand if board has fewer than 3 low cards`() {
        // User scenario:
        // Hole: Qh, 4d, 2s, Ad (Low candidates: 4, 2, A)
        val hole = listOf(
            Card(Rank.QUEEN, Suit.HEARTS),
            Card(Rank.FOUR, Suit.DIAMONDS),
            Card(Rank.TWO, Suit.SPADES),
            Card(Rank.ACE, Suit.DIAMONDS),
        )

        // Community: Qs, 5h, 3c (Low candidates: 5, 3)
        // Total low cards available: A, 2, 4 (hole) + 5, 3 (comm) = 5 cards.
        // BUT we need 3 from community. Only 5 and 3 are there.
        val community = listOf(
            Card(Rank.QUEEN, Suit.SPADES),
            Card(Rank.FIVE, Suit.HEARTS),
            Card(Rank.THREE, Suit.CLUBS),
        )

        val result = evaluator.findBestHand(hole, community)

        // Expecting only 1 hand (High), no Low hand.
        // High hand should be One Pair Queens.

        assertEquals(1, result.size, "Should only have high hand")
        assertTrue(result[0].description().contains("Pair"), "High hand should be a Pair")

        // If low hand was found, size would be 2.
    }
}
