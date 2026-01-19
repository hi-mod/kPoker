package com.aaronchancey.poker.presentation.lobby

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.shared.message.RoomInfo

/**
 * Lobby screen for room selection.
 *
 * @param state Current lobby state
 * @param onJoinRoom Callback when a room is selected (roomInfo, playerName)
 * @param onRefresh Callback to refresh the room list
 */
@Composable
fun Lobby(
    state: LobbyState,
    onJoinRoom: (RoomInfo, String) -> Unit,
    onRefresh: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        val playerNameState = rememberTextFieldState("")
        TextField(
            state = playerNameState,
            label = { Text("Enter your name") },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onRefresh) {
            Text("Refresh Rooms")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.rooms.isEmpty() && !state.isLoading) {
            Text("No rooms available", style = MaterialTheme.typography.bodyMedium)
        }

        state.rooms.forEach { room ->
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                RoomItem(
                    room = room,
                    onClick = {
                        val playerName = playerNameState.text.toString().ifBlank { "Player" }
                        onJoinRoom(room, playerName)
                    },
                )
            }
        }
    }
}

@Composable
private fun RoomItem(
    modifier: Modifier = Modifier,
    room: RoomInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(room.roomName, style = MaterialTheme.typography.titleMedium)
            Text(room.variant.displayName, style = MaterialTheme.typography.bodySmall)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${room.smallBlind}/${room.bigBlind}", style = MaterialTheme.typography.bodyMedium)
            Text("${room.playerCount}/${room.maxPlayers} players", style = MaterialTheme.typography.bodySmall)
        }
    }
}
