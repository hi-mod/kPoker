package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * - Leave Seat and Disconnect buttons (below table, collapsed to menu on mobile)
 *
 * @param isMobile Whether to use compact mobile layout
 * @param uiState Current room UI state
 * @param onIntent Handler for room intents (actions, seat management, etc.)
 */
@Composable
fun ColumnScope.RoomControls(
    isMobile: Boolean,
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

    if (isMobile) {
        MobileRoomMenu(
            isLoading = uiState.isLoading,
            onIntent = onIntent,
        )
    } else {
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
}

/**
 * Collapsed menu icon for room actions on mobile viewports.
 * Saves vertical space by replacing the full button row with a dropdown.
 */
@Composable
private fun MobileRoomMenu(
    isLoading: Boolean,
    onIntent: (RoomIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            modifier = Modifier.size(44.dp),
            onClick = { expanded = true },
        ) {
            Text("⋮")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Leave Seat") },
                onClick = {
                    expanded = false
                    onIntent(RoomIntent.LeaveSeat)
                },
                enabled = !isLoading,
            )
            DropdownMenuItem(
                text = { Text("Disconnect") },
                onClick = {
                    expanded = false
                    onIntent(RoomIntent.Disconnect)
                },
                enabled = !isLoading,
            )
        }
    }
}
