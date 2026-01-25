package com.aaronchancey.poker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState
import com.aaronchancey.poker.presentation.room.components.ShowPlayers

@Composable
fun RoomTable(
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
) {
    uiState.gameState?.let { state ->
        Text("Pot: ${state.totalPot}")

        Spacer(modifier = Modifier.Companion.height(16.dp))

        ShowPlayers(
            isLoading = uiState.isLoading,
            uiState = uiState,
            onTakeSeat = { onIntent(RoomIntent.TakeSeat(it, 100.0)) },
            onIntent = onIntent,
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { onIntent(RoomIntent.LeaveSeat) },
            enabled = !uiState.isLoading,
        ) {
            Text("Leave Seat")
        }
        Spacer(modifier = Modifier.Companion.width(8.dp))
        Button(
            onClick = { onIntent(RoomIntent.Disconnect) },
            enabled = !uiState.isLoading,
        ) {
            Text("Disconnect")
        }
    }
}
