package com.aaronchancey.poker.kpoker.equity

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.game.GameVariant
import kotlin.test.Test
import kotlin.test.assertTrue

class EquityCalculatorTest {

    /** AA vs 1 random opponent should be ~85% equity. */
    @Test
    fun pocketAcesVsOneOpponent() {
        val equity = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("As"), Card.fromString("Ah")),
            communityCards = emptyList(),
            opponentCount = 1,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 5000,
        )
        assertTrue(equity > 0.80, "AA equity should be > 80%, was $equity")
        assertTrue(equity < 0.92, "AA equity should be < 92%, was $equity")
    }

    /** 72o vs 1 random opponent should be ~35% equity. */
    @Test
    fun sevenTwoOffVsOneOpponent() {
        val equity = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("7s"), Card.fromString("2h")),
            communityCards = emptyList(),
            opponentCount = 1,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 5000,
        )
        assertTrue(equity > 0.28, "72o equity should be > 28%, was $equity")
        assertTrue(equity < 0.42, "72o equity should be < 42%, was $equity")
    }

    /** Nut flush draw on the flop — 4 suited cards with 2 to come. ~35% equity. */
    @Test
    fun nutFlushDrawOnFlop() {
        val equity = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("As"), Card.fromString("Ks")),
            communityCards = listOf(
                Card.fromString("7s"),
                Card.fromString("4s"),
                Card.fromString("Td"),
            ),
            opponentCount = 1,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 5000,
        )
        // AKs with nut flush draw + overcards is very strong — roughly 50-75%
        assertTrue(equity > 0.40, "Nut flush draw + overcards equity should be > 40%, was $equity")
        assertTrue(equity < 0.80, "Nut flush draw + overcards equity should be < 80%, was $equity")
    }

    /** Equity against multiple opponents should be lower than heads-up. */
    @Test
    fun equityDecreasesWithMoreOpponents() {
        val headsUp = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("Kh"), Card.fromString("Qh")),
            communityCards = emptyList(),
            opponentCount = 1,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 3000,
        )
        val multiway = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("Kh"), Card.fromString("Qh")),
            communityCards = emptyList(),
            opponentCount = 4,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 3000,
        )
        assertTrue(
            headsUp > multiway,
            "KQs equity heads-up ($headsUp) should exceed 4-way ($multiway)",
        )
    }

    /** On the river (5 community cards), no more cards to deal — just opponent hands. */
    @Test
    fun riverEquityWithFullBoard() {
        val equity = EquityCalculator.calculateEquity(
            heroHoleCards = listOf(Card.fromString("As"), Card.fromString("Ah")),
            communityCards = listOf(
                Card.fromString("Ac"),
                Card.fromString("Kd"),
                Card.fromString("7s"),
                Card.fromString("3h"),
                Card.fromString("2d"),
            ),
            opponentCount = 1,
            variant = GameVariant.TEXAS_HOLDEM,
            iterations = 5000,
        )
        // Trip aces on a dry board — should be very high
        assertTrue(equity > 0.90, "Trip aces on river should be > 90%, was $equity")
    }
}
