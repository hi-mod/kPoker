package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ActionType
import com.aaronchancey.poker.kpoker.betting.BettingType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PreActionTest {

    private fun actionRequest(
        amountToCall: Double = 0.0,
        validActions: Set<ActionType> = setOf(ActionType.CHECK, ActionType.BET, ActionType.ALL_IN),
    ) = ActionRequest(
        playerId = "p1",
        validActions = validActions,
        minimumDenomination = 0.1,
        minimumBet = 2.0,
        minimumRaise = 2.0,
        maximumBet = 100.0,
        amountToCall = amountToCall,
        bettingType = BettingType.NO_LIMIT,
    )

    // === CHECK ===

    @Test
    fun checkReturnsCheckWhenNoAmountToCall() {
        val result = resolvePreAction(
            preAction = PreActionType.CHECK,
            actionRequest = actionRequest(amountToCall = 0.0),
            snapshotBet = 0.0,
        )
        assertIs<Action.Check>(result)
    }

    @Test
    fun checkReturnsNullWhenBetCameIn() {
        val result = resolvePreAction(
            preAction = PreActionType.CHECK,
            actionRequest = actionRequest(
                amountToCall = 4.0,
                validActions = setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            ),
            snapshotBet = 0.0,
        )
        assertNull(result, "CHECK should be invalidated when a bet comes in")
    }

    // === CHECK_FOLD ===

    @Test
    fun checkFoldChecksWhenPossible() {
        val result = resolvePreAction(
            preAction = PreActionType.CHECK_FOLD,
            actionRequest = actionRequest(amountToCall = 0.0),
            snapshotBet = 0.0,
        )
        assertIs<Action.Check>(result)
    }

    @Test
    fun checkFoldFoldsWhenBetCameIn() {
        val result = resolvePreAction(
            preAction = PreActionType.CHECK_FOLD,
            actionRequest = actionRequest(
                amountToCall = 4.0,
                validActions = setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            ),
            snapshotBet = 0.0,
        )
        assertIs<Action.Fold>(result, "CHECK_FOLD should fold when a bet comes in")
    }

    // === CALL ===

    @Test
    fun callReturnsCallWhenBetUnchanged() {
        val result = resolvePreAction(
            preAction = PreActionType.CALL,
            actionRequest = actionRequest(
                amountToCall = 4.0,
                validActions = setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            ),
            snapshotBet = 4.0,
        )
        assertIs<Action.Call>(result)
        assertEquals(4.0, result.amount)
    }

    @Test
    fun callReturnsNullWhenBetIncreased() {
        val result = resolvePreAction(
            preAction = PreActionType.CALL,
            actionRequest = actionRequest(
                amountToCall = 8.0,
                validActions = setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            ),
            snapshotBet = 4.0,
        )
        assertNull(result, "CALL should be invalidated when bet increased")
    }

    @Test
    fun callReturnsNullWhenNothingToCall() {
        val result = resolvePreAction(
            preAction = PreActionType.CALL,
            actionRequest = actionRequest(amountToCall = 0.0),
            snapshotBet = 0.0,
        )
        assertNull(result, "CALL should be invalidated when there's nothing to call")
    }

    // === CALL_ANY ===

    @Test
    fun callAnyReturnsCallRegardlessOfBetChange() {
        val result = resolvePreAction(
            preAction = PreActionType.CALL_ANY,
            actionRequest = actionRequest(
                amountToCall = 20.0,
                validActions = setOf(ActionType.FOLD, ActionType.CALL, ActionType.RAISE, ActionType.ALL_IN),
            ),
            snapshotBet = 4.0,
        )
        assertIs<Action.Call>(result)
        assertEquals(20.0, result.amount, "CALL_ANY should call the new amount")
    }

    @Test
    fun callAnyReturnsNullWhenNothingToCall() {
        val result = resolvePreAction(
            preAction = PreActionType.CALL_ANY,
            actionRequest = actionRequest(amountToCall = 0.0),
            snapshotBet = 4.0,
        )
        assertNull(result, "CALL_ANY should be invalidated when there's nothing to call")
    }
}
