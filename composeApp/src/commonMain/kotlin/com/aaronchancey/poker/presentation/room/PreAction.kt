package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.player.ChipAmount

/**
 * Pre-action selections the player can make before their turn.
 *
 * These are purely client-side - never sent to the server until it's the player's turn.
 * When the player's turn arrives, the selected pre-action is validated against the
 * current [ActionRequest] and auto-submitted if still valid.
 */
enum class PreActionType {
    /** Auto-check if possible, otherwise fold. */
    CHECK_FOLD,

    /** Auto-check (invalidated if a bet comes in). */
    CHECK,

    /** Auto-call the current bet amount. */
    CALL,

    /** Auto-call regardless of bet size changes. */
    CALL_ANY,
}

/**
 * Resolves a pre-action selection into a concrete [Action] given the current action request,
 * or returns null if the pre-action is no longer valid.
 *
 * @param preAction The pre-action the player selected
 * @param actionRequest The server's action request for this player's turn
 * @param snapshotBet The bet amount at the time the pre-action was selected (for invalidation)
 * @return The resolved [Action] to auto-submit, or null if the pre-action is invalid
 */
fun resolvePreAction(
    preAction: PreActionType,
    actionRequest: ActionRequest,
    snapshotBet: ChipAmount,
) = when (preAction) {
    PreActionType.CHECK,
    PreActionType.CHECK_FOLD,
    -> checkFold(preAction, actionRequest)
    PreActionType.CALL,
    PreActionType.CALL_ANY,
    -> callAny(preAction, actionRequest, snapshotBet)
}

private fun callAny(
    preAction: PreActionType,
    actionRequest: ActionRequest,
    snapshotBet: ChipAmount,
): Action? {
    // Can't call when there's nothing to call - invalidate
    if (actionRequest.amountToCall <= 0.0) return null

    return if (actionRequest.amountToCall == snapshotBet || preAction == PreActionType.CALL_ANY) {
        Action.Call(actionRequest.playerId, actionRequest.amountToCall)
    } else {
        null
    }
}

private fun checkFold(
    preAction: PreActionType,
    actionRequest: ActionRequest,
): Action? = if (actionRequest.amountToCall <= 0.0) {
    Action.Check(actionRequest.playerId)
} else {
    if (preAction == PreActionType.CHECK_FOLD) {
        Action.Fold(actionRequest.playerId)
    } else {
        null
    }
}
