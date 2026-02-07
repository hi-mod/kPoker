package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.kpoker.equity.ActionEv
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.presentation.room.PreActionType
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState

/**
 * Bottom action bar displaying player actions, pre-action checkboxes, and hand description.
 *
 * Shows:
 * - **Player actions** when it's the player's turn
 * - **Pre-action checkboxes** when the player is in the hand, but it's NOT their turn
 * - Nothing when the player is not in the hand
 *
 * @param currentActor The player whose turn it is, or null if no active turn
 * @param uiState Current room UI state for action context
 * @param handDescription Text describing the current hand strength
 * @param onIntent Handler for player action intents
 */
@Composable
fun ActionBar(
    modifier: Modifier = Modifier,
    currentActor: PlayerState?,
    uiState: RoomUiState,
    handDescription: String,
    onIntent: (RoomIntent) -> Unit,
) {
    val myPlayerId = uiState.playerId
    val isMyTurn = currentActor?.player?.id == myPlayerId
    val myPlayerState = myPlayerId?.let { id ->
        uiState.gameState?.table?.getPlayerSeat(id)?.playerState
    }
    val isInHand = myPlayerState?.status in listOf(PlayerStatus.ACTIVE, PlayerStatus.ALL_IN)

    Row(
        modifier = modifier.heightIn(min = 105.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            isMyTurn && currentActor != null -> {
                PlayerActions(
                    playerState = currentActor,
                    uiState = uiState,
                    onIntent = onIntent,
                )
            }

            isInHand && !isMyTurn && canShowPreActions(uiState) -> {
                PreActionCheckboxes(
                    selectedPreAction = uiState.selectedPreAction,
                    onSelectPreAction = { onIntent(RoomIntent.SelectPreAction(it)) },
                )
            }
        }

        if (myPlayerState != null) {
            Spacer(Modifier.weight(1f))
            SitOutToggle(
                isSittingOut = myPlayerState.status == PlayerStatus.SITTING_OUT ||
                    myPlayerState.sitOutNextHand,
                isInHand = isInHand,
                onToggle = { onIntent(RoomIntent.ToggleSitOut) },
            )
        }

        if (handDescription.isNotEmpty() || uiState.actionEv != null) {
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                if (handDescription.isNotEmpty()) {
                    Text(handDescription)
                }
                uiState.actionEv?.let { ev ->
                    EvDisplay(actionEv = ev)
                }
            }
        }
    }
}

/**
 * Pre-action checkboxes shown when it's NOT the player's turn.
 * Selecting a checkbox queues an action for auto-submission when the turn arrives.
 */
@Composable
private fun PreActionCheckboxes(
    selectedPreAction: PreActionType?,
    onSelectPreAction: (PreActionType?) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreActionType.entries.forEach { preAction ->
            val isSelected = selectedPreAction == preAction
            Checkbox(
                checked = isSelected,
                onCheckedChange = { checked ->
                    onSelectPreAction(if (checked) preAction else null)
                },
            )
            Text(preAction.label)
        }
    }
}

/**
 * Toggle button for sitting out / sitting back in.
 *
 * Shows "Sit Out Next Hand" when in an active hand (sets [PlayerState.sitOutNextHand]),
 * "Sit Out" / "Sit In" when between hands.
 */
@Composable
private fun SitOutToggle(
    isSittingOut: Boolean,
    isInHand: Boolean,
    onToggle: () -> Unit,
) {
    val label = when {
        isSittingOut && isInHand -> "Cancel Sit Out"
        isSittingOut -> "Sit In"
        isInHand -> "Sit Out Next Hand"
        else -> "Sit Out"
    }
    Button(
        onClick = onToggle,
        colors = if (isSittingOut) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Text(label)
    }
}

/**
 * Displays equity percentage and call/check EV inline.
 * Positive EV is shown in tertiary color, negative in error color.
 */
@Composable
private fun EvDisplay(actionEv: ActionEv) {
    val equityText = "Equity: ${(actionEv.equity * 100).toInt()}%"
    val callEv = actionEv.callEv
    val checkEv = actionEv.checkEv
    val evPart = when {
        callEv != null -> " | Call EV: ${formatEv(callEv)}"
        checkEv != null -> " | Check EV: ${formatEv(checkEv)}"
        else -> ""
    }
    val evValue = callEv ?: checkEv
    val evColor = when {
        evValue == null -> MaterialTheme.colorScheme.onSurface
        evValue >= 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Text(
        text = equityText + evPart,
        style = MaterialTheme.typography.bodySmall,
        color = evColor,
    )
}

private fun canShowPreActions(uiState: RoomUiState): Boolean {
    val phase = uiState.gameState?.phase
    return uiState.showdown == null && phase != GamePhase.HAND_COMPLETE && phase != GamePhase.SHOWDOWN
}

private fun formatEv(ev: Double): String {
    val sign = if (ev >= 0) "+" else ""
    val rounded = (ev * 10).toLong() / 10.0
    return "$sign$rounded"
}

private val PreActionType.label: String
    get() = when (this) {
        PreActionType.CHECK_FOLD -> "Check/Fold"
        PreActionType.CHECK -> "Check"
        PreActionType.CALL -> "Call"
        PreActionType.CALL_ANY -> "Call Any"
    }
