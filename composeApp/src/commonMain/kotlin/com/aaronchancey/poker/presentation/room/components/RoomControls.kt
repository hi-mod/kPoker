package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState

/**
 * Room controls layout containing the poker table and room action buttons.
 *
 * Must be called within a [ColumnScope] so the table can use `weight(1f)`
 * to fill remaining space while leaving room for the button row.
 *
 * Displays:
 * - [PokerTableScene] with full game view (weighted to fill available space)
 * - Leave Seat and Disconnect buttons (below table)
 *
 * @param uiState Current room UI state
 * @param onIntent Handler for room intents (actions, seat management, etc.)
 */
@Composable
fun ColumnScope.RoomControls(
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
) {
    val takeSeatHandler = remember(onIntent) {
        { seatNumber: Int ->
            onIntent(RoomIntent.TakeSeat(seatNumber, 100.0))
        }
    }

    PokerTableScene(
        modifier = Modifier.weight(1f).fillMaxSize(),
        isLoading = uiState.isLoading,
        uiState = uiState,
        onTakeSeat = takeSeatHandler,
        onIntent = onIntent,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onIntent(RoomIntent.LeaveSeat) },
            enabled = !uiState.isLoading,
        ) {
            Text("Leave Seat")
        }
        Button(
            onClick = { onIntent(RoomIntent.Disconnect) },
            enabled = !uiState.isLoading,
        ) {
            Text("Disconnect")
        }
    }
}
