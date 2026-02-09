package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

    val scale = LocalTableScale.current

    if (scale.isMobile) {
        MobileActionBar(
            modifier = modifier.heightIn(min = scale.actionBarMinHeight),
            currentActor = currentActor,
            isMyTurn = isMyTurn,
            isInHand = isInHand,
            myPlayerState = myPlayerState,
            uiState = uiState,
            handDescription = handDescription,
            onIntent = onIntent,
        )
    } else {
        DesktopActionBar(
            modifier = modifier.heightIn(min = scale.actionBarMinHeight),
            currentActor = currentActor,
            isMyTurn = isMyTurn,
            isInHand = isInHand,
            myPlayerState = myPlayerState,
            uiState = uiState,
            handDescription = handDescription,
            onIntent = onIntent,
        )
    }
}

/** Desktop layout: single Row with all elements horizontally. */
@Composable
private fun DesktopActionBar(
    modifier: Modifier,
    currentActor: PlayerState?,
    isMyTurn: Boolean,
    isInHand: Boolean,
    myPlayerState: PlayerState?,
    uiState: RoomUiState,
    handDescription: String,
    onIntent: (RoomIntent) -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionContent(
            currentActor = currentActor,
            isMyTurn = isMyTurn,
            isInHand = isInHand,
            uiState = uiState,
            onIntent = onIntent,
        )

        if (myPlayerState != null) {
            Spacer(Modifier.weight(1f))
            SitOutToggle(
                isSittingOut = myPlayerState.status == PlayerStatus.SITTING_OUT ||
                    myPlayerState.sitOutNextHand,
                isInHand = isInHand,
                isMobile = false,
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
 * Mobile layout: stacks actions on top, info row on bottom.
 *
 * ```
 * ┌──────────────────────────────┐
 * │  PlayerActions / PreActions  │
 * │  [SitOut]     HandDesc / EV  │
 * └──────────────────────────────┘
 * ```
 */
@Composable
private fun MobileActionBar(
    modifier: Modifier,
    currentActor: PlayerState?,
    isMyTurn: Boolean,
    isInHand: Boolean,
    myPlayerState: PlayerState?,
    uiState: RoomUiState,
    handDescription: String,
    onIntent: (RoomIntent) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Primary row: actions or pre-action checkboxes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionContent(
                currentActor = currentActor,
                isMyTurn = isMyTurn,
                isInHand = isInHand,
                uiState = uiState,
                onIntent = onIntent,
            )
        }

        // Secondary row: sit-out toggle + hand description
        val hasInfo = myPlayerState != null ||
            handDescription.isNotEmpty() ||
            uiState.actionEv != null
        if (hasInfo) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (myPlayerState != null) {
                    SitOutToggle(
                        isSittingOut = myPlayerState.status == PlayerStatus.SITTING_OUT ||
                            myPlayerState.sitOutNextHand,
                        isInHand = isInHand,
                        isMobile = true,
                        onToggle = { onIntent(RoomIntent.ToggleSitOut) },
                    )
                }

                Spacer(Modifier.weight(1f))

                if (handDescription.isNotEmpty() || uiState.actionEv != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (handDescription.isNotEmpty()) {
                            Text(
                                text = handDescription,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        uiState.actionEv?.let { ev ->
                            EvDisplay(
                                actionEv = ev,
                                isMobile = true,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Shared action/pre-action content used by both mobile and desktop layouts. */
@Composable
private fun ActionContent(
    currentActor: PlayerState?,
    isMyTurn: Boolean,
    isInHand: Boolean,
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
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
    val scale = LocalTableScale.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(scale.buttonSpacing),
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
            Text(preAction.label(isMobile = scale.isMobile))
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
    isMobile: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val label = when {
        isMobile -> "Sit Out"
        isInHand -> "Sit Out Next Hand"
        else -> "Sit Out"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = isSittingOut,
            onCheckedChange = onToggle,
        )
        Text(label)
    }
}

/**
 * Displays equity percentage and call/check EV inline.
 * Positive EV is shown in tertiary color, negative in error color.
 */
@Composable
private fun EvDisplay(
    actionEv: ActionEv,
    isMobile: Boolean = false,
) {
    val equityPct = (actionEv.equity * 100).toInt()
    val callEv = actionEv.callEv
    val checkEv = actionEv.checkEv

    val text = if (isMobile) {
        val evPart = when {
            callEv != null -> " | C: ${formatEv(callEv)}"
            checkEv != null -> " | X: ${formatEv(checkEv)}"
            else -> ""
        }
        "Eq: $equityPct%$evPart"
    } else {
        val evPart = when {
            callEv != null -> " | Call EV: ${formatEv(callEv)}"
            checkEv != null -> " | Check EV: ${formatEv(checkEv)}"
            else -> ""
        }
        "Equity: $equityPct%$evPart"
    }

    val evValue = callEv ?: checkEv
    val evColor = when {
        evValue == null -> MaterialTheme.colorScheme.onSurface
        evValue >= 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = evColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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

private fun PreActionType.label(isMobile: Boolean): String = when (this) {
    PreActionType.CHECK_FOLD -> if (isMobile) "C/F" else "Check/Fold"
    PreActionType.CHECK -> if (isMobile) "Chk" else "Check"
    PreActionType.CALL -> "Call"
    PreActionType.CALL_ANY -> if (isMobile) "Any" else "Call Any"
}
