package com.aaronchancey.poker.presentation.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.game.GameIntent
import com.aaronchancey.poker.shared.message.RoomInfo

@Composable
fun RoomsScreen(
    state: RoomState,
    onIntent: (GameIntent) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var playerName by remember { mutableStateOf("") }
        TextField(playerName, onValueChange = { playerName = it })
        state.rooms.forEach { room ->
            Card(
                modifier = Modifier.padding(4.dp),
                elevation = CardDefaults.cardElevation(8.dp),
            ) {
                RoomItem(
                    room = room,
                    onClick = {
                        onIntent(GameIntent.JoinRoom(playerName = playerName, roomId = room.roomId))
                    },
                )
            }
        }
    }
}

@Composable
fun RoomItem(
    modifier: Modifier = Modifier,
    room: RoomInfo,
    onClick: (String) -> Unit,
) {
    Row(
        modifier = modifier.clickable { onClick(room.roomId) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(room.roomName)
        Text(room.variant.displayName)
        Text("${room.smallBlind}/${room.bigBlind}")
        Text("${room.playerCount}/${room.maxPlayers} players")
    }
}
