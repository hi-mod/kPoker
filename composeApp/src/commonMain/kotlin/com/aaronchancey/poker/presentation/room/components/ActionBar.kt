package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * - **Pre-action checkboxes** when the player is in the hand but it's NOT their turn
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
                Spacer(Modifier.weight(1f))
                Text(handDescription)
            }

            isInHand && !isMyTurn && uiState.showdown == null && !listOf(GamePhase.HAND_COMPLETE, GamePhase.SHOWDOWN).contains(uiState.gameState?.phase) -> {
                PreActionCheckboxes(
                    selectedPreAction = uiState.selectedPreAction,
                    onSelectPreAction = { onIntent(RoomIntent.SelectPreAction(it)) },
                )
                Spacer(Modifier.weight(1f))
                Text(handDescription)
            }

            else -> {
                if (handDescription.isNotEmpty()) {
                    Text(handDescription)
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

private val PreActionType.label: String
    get() = when (this) {
        PreActionType.CHECK_FOLD -> "Check/Fold"
        PreActionType.CHECK -> "Check"
        PreActionType.CALL -> "Call"
        PreActionType.CALL_ANY -> "Call Any"
    }
