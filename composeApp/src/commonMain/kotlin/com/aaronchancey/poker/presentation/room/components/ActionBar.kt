package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState

/**
 * Bottom action bar displaying player actions and hand description.
 *
 * Only renders content when there's a current actor (player whose turn it is).
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
    Row(
        modifier = modifier.heightIn(min = 105.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        currentActor?.let { actor ->
            PlayerActions(
                playerState = actor,
                uiState = uiState,
                onIntent = onIntent,
            )
            Text(handDescription)
        }
    }
}
