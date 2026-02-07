package com.aaronchancey.poker.kpoker.equity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActionEvCalculatorTest {

    @Test
    fun foldEvIsAlwaysZero() {
        val result = ActionEvCalculator.calculate(
            equity = 0.5,
            potSize = 100.0,
            amountToCall = 10.0,
            canCheck = false,
            canCall = true,
        )
        assertEquals(0.0, result.foldEv)
    }

    @Test
    fun checkEvEqualsEquityTimesPot() {
        val result = ActionEvCalculator.calculate(
            equity = 0.6,
            potSize = 100.0,
            amountToCall = 0.0,
            canCheck = true,
            canCall = false,
        )
        val checkEv = assertNotNull(result.checkEv)
        assertEquals(60.0, checkEv, 0.001)
        assertNull(result.callEv)
    }

    @Test
    fun callEvUsesCorrectFormula() {
        // EV = equity * (pot + call) - call
        // EV = 0.4 * (100 + 20) - 20 = 48 - 20 = 28
        val result = ActionEvCalculator.calculate(
            equity = 0.4,
            potSize = 100.0,
            amountToCall = 20.0,
            canCheck = false,
            canCall = true,
        )
        val callEv = assertNotNull(result.callEv)
        assertEquals(28.0, callEv, 0.001)
        assertNull(result.checkEv)
    }

    @Test
    fun negativeCallEvWhenEquityTooLow() {
        // EV = 0.1 * (100 + 50) - 50 = 15 - 50 = -35
        val result = ActionEvCalculator.calculate(
            equity = 0.1,
            potSize = 100.0,
            amountToCall = 50.0,
            canCheck = false,
            canCall = true,
        )
        val callEv = assertNotNull(result.callEv)
        assertEquals(-35.0, callEv, 0.001)
    }

    @Test
    fun bothCheckAndCallAvailable() {
        val result = ActionEvCalculator.calculate(
            equity = 0.5,
            potSize = 80.0,
            amountToCall = 10.0,
            canCheck = true,
            canCall = true,
        )
        val checkEv = assertNotNull(result.checkEv)
        val callEv = assertNotNull(result.callEv)
        assertEquals(40.0, checkEv, 0.001)
        // 0.5 * (80 + 10) - 10 = 45 - 10 = 35
        assertEquals(35.0, callEv, 0.001)
    }

    @Test
    fun neitherCheckNorCallAvailable() {
        val result = ActionEvCalculator.calculate(
            equity = 0.5,
            potSize = 80.0,
            amountToCall = 0.0,
            canCheck = false,
            canCall = false,
        )
        assertNull(result.checkEv)
        assertNull(result.callEv)
        assertEquals(0.5, result.equity)
    }

    @Test
    fun equityIsPassedThrough() {
        val result = ActionEvCalculator.calculate(
            equity = 0.73,
            potSize = 50.0,
            amountToCall = 0.0,
            canCheck = true,
            canCall = false,
        )
        assertEquals(0.73, result.equity, 0.001)
    }

    @Test
    fun breakEvenCallEquity() {
        // At break-even: equity * (pot + call) = call
        // equity = call / (pot + call) = 20 / 120 = 0.1667
        val breakEvenEquity = 20.0 / 120.0
        val result = ActionEvCalculator.calculate(
            equity = breakEvenEquity,
            potSize = 100.0,
            amountToCall = 20.0,
            canCheck = false,
            canCall = true,
        )
        val callEv = assertNotNull(result.callEv)
        assertEquals(0.0, callEv, 0.001)
    }
}
