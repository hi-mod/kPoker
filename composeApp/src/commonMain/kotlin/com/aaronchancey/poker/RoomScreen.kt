package com.aaronchancey.poker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.presentation.room.RoomEffect
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState
import com.aaronchancey.poker.presentation.room.components.ShowPlayers
import com.aaronchancey.poker.presentation.sound.SoundManager
import com.aaronchancey.poker.presentation.sound.SoundPlayer
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

/**
 * Standalone room composable for the multi-window architecture.
 *
 * This is the main entry point for a room window. It handles:
 * - Displaying the game state
 * - Notifying when disconnected
 *
 * Room joining is handled automatically by [RoomViewModel] when its
 * uiState is collected, using [RoomParams] passed via construction.
 *
 * @param uiState Current UI state from ViewModel
 * @param effects Side effects flow from ViewModel
 * @param onDisconnected Callback when the player disconnects (to close window)
 * @param onIntent Handler for user intents
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RoomScreen(
    uiState: RoomUiState,
    effects: Flow<RoomEffect>,
    onDisconnected: () -> Unit,
    onIntent: (RoomIntent) -> Unit,
) = MaterialExpressiveTheme {
    // Handle side effects
    val soundPlayer: SoundPlayer = koinInject()
    LaunchedEffect(Unit) {
        effects.collect { effect ->
            when (effect) {
                is RoomEffect.ShowToast -> {
                    // Platform-specific toast
                }

                is RoomEffect.NavigateToLobby -> {
                    onDisconnected()
                }

                is RoomEffect.PlaySound -> {
                    val path = SoundManager.getPath(effect.soundType)
                    soundPlayer.playSound(path)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState.connectionState) {
            ConnectionState.DISCONNECTED -> {
                CircularProgressIndicator()
                Text("Connecting...")
            }

            ConnectionState.CONNECTING -> {
                CircularProgressIndicator()
                Text("Connecting...")
            }

            ConnectionState.RECONNECTING -> {
                CircularProgressIndicator()
                Text("Reconnecting...")
            }

            ConnectionState.RECONNECTED,
            ConnectionState.CONNECTED,
            -> {
                if (uiState.roomInfo != null) {
                    RoomGameScreen(
                        modifier = Modifier.fillMaxSize(),
                        uiState = uiState,
                        onIntent = onIntent,
                    )
                } else {
                    CircularProgressIndicator()
                    Text("Loading room...")
                }
            }
        }

        uiState.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Button(onClick = { onIntent(RoomIntent.ClearError) }) {
                Text("Dismiss")
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun RoomGameScreen(
    modifier: Modifier = Modifier,
    uiState: RoomUiState,
    onIntent: (RoomIntent) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        uiState.roomInfo?.let { room ->
            Text("Room: ${room.roomName}", style = MaterialTheme.typography.headlineSmall)
            Text("Hand No: ${uiState.gameState?.handNumber ?: "N/A"}")
            Text("Blinds: ${room.smallBlind}/${room.bigBlind}")
            Text("Players: ${room.playerCount}/${room.maxPlayers}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        uiState.gameState?.let { state ->
            Text("Phase: ${state.phase}")
            Text("Pot: ${state.totalPot}")

            Spacer(modifier = Modifier.height(16.dp))

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
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onIntent(RoomIntent.Disconnect) },
                enabled = !uiState.isLoading,
            ) {
                Text("Disconnect")
            }
        }
    }
}
