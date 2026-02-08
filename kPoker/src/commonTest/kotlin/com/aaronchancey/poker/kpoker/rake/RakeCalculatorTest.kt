package com.aaronchancey.poker.kpoker.rake

import com.aaronchancey.poker.kpoker.player.Pot
import kotlin.test.Test
import kotlin.test.assertEquals

class RakeCalculatorTest {

    @Test
    fun `basic percentage calculation`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 10.0,
            minDenomination = 1.0,
        )
        val result = calc.calculateRake(listOf(Pot(amount = 100.0, eligiblePlayerIds = setOf("a", "b"))))
        assertEquals(listOf(5.0), result)
    }

    @Test
    fun `rake is capped per pot`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 3.0,
            minDenomination = 1.0,
        )
        // 5% of 200 = 10, but capped at 3
        val result = calc.calculateRake(listOf(Pot(amount = 200.0, eligiblePlayerIds = setOf("a", "b"))))
        assertEquals(listOf(3.0), result)
    }

    @Test
    fun `denomination rounding floors to nearest denomination`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 10.0,
            minDenomination = 0.25,
        )
        // 5% of 73 = 3.65, floored to nearest 0.25 = 3.50
        val result = calc.calculateRake(listOf(Pot(amount = 73.0, eligiblePlayerIds = setOf("a", "b"))))
        assertEquals(listOf(3.5), result)
    }

    @Test
    fun `denomination rounding with whole number denomination`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 10.0,
            minDenomination = 1.0,
        )
        // 5% of 73 = 3.65, floored to nearest 1.0 = 3.0
        val result = calc.calculateRake(listOf(Pot(amount = 73.0, eligiblePlayerIds = setOf("a", "b"))))
        assertEquals(listOf(3.0), result)
    }

    @Test
    fun `multiple pots raked independently`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 3.0,
            minDenomination = 1.0,
        )
        val pots = listOf(
            Pot(amount = 100.0, eligiblePlayerIds = setOf("a", "b"), isMain = true),
            Pot(amount = 50.0, eligiblePlayerIds = setOf("a"), isMain = false),
        )
        // Main: 5% of 100 = 5, capped at 3
        // Side: 5% of 50 = 2.5, floored to 2
        val result = calc.calculateRake(pots)
        assertEquals(listOf(3.0, 2.0), result)
    }

    @Test
    fun `empty pots list returns empty`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 10.0,
            minDenomination = 1.0,
        )
        assertEquals(emptyList(), calc.calculateRake(emptyList()))
    }

    @Test
    fun `zero amount pot returns zero rake`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.05,
            rakeCap = 10.0,
            minDenomination = 1.0,
        )
        val result = calc.calculateRake(listOf(Pot(amount = 0.0, eligiblePlayerIds = setOf("a"))))
        assertEquals(listOf(0.0), result)
    }

    @Test
    fun `cap applied before denomination rounding`() {
        val calc = PercentageRakeCalculator(
            rakePercent = 0.10,
            rakeCap = 2.30,
            minDenomination = 0.25,
        )
        // 10% of 100 = 10, capped at 2.30, then floored to 2.25
        val result = calc.calculateRake(listOf(Pot(amount = 100.0, eligiblePlayerIds = setOf("a", "b"))))
        assertEquals(listOf(2.25), result)
    }
}
