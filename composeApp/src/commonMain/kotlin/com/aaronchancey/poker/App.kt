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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.presentation.lobby.Lobby
import com.aaronchancey.poker.presentation.lobby.LobbyIntent
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.RoomEffect
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomParams
import com.aaronchancey.poker.presentation.room.RoomUiState
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.presentation.room.components.ShowPlayers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf

/**
 * Single-window App composable for platforms like Android.
 *
 * Uses internal navigation state to switch between Lobby and Room screens.
 * RoomViewModel is created with RoomParams when a room is selected.
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    KoinExperimentalAPI::class,
    kotlin.uuid.ExperimentalUuidApi::class,
)
@Composable
fun App() = MaterialExpressiveTheme {
    // Track current room params - null means show lobby
    var currentRoomParams: RoomParams? by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val roomParams = currentRoomParams
        if (roomParams == null) {
            // Show Lobby - rooms are fetched automatically via onStart in LobbyViewModel
            val lobbyViewModel = koinViewModel<LobbyViewModel>()
            val lobbyState by lobbyViewModel.state.collectAsState()

            Lobby(
                state = lobbyState,
                onJoinRoom = { roomInfo, playerName ->
                    val playerId = kotlin.uuid.Uuid.random().toString()
                    currentRoomParams = RoomParams(
                        roomId = roomInfo.roomId,
                        playerName = playerName,
                        playerId = playerId,
                    )
                },
                onRefresh = { lobbyViewModel.onIntent(LobbyIntent.RefreshRooms) },
            )
        } else {
            // Show Room
            val viewModel = koinViewModel<RoomViewModel>(
                key = roomParams.roomId,
                parameters = { parametersOf(roomParams) },
            )
            val uiState by viewModel.uiState.collectAsState()

            // Handle side effects
            LaunchedEffect(Unit) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is RoomEffect.ShowToast -> {
                            // Platform-specific toast implementation
                        }

                        is RoomEffect.NavigateToLobby -> {
                            currentRoomParams = null
                        }

                        is RoomEffect.PlaySound -> {
                            // Sound playback
                        }
                    }
                }
            }

            when (uiState.connectionState) {
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTING,
                -> {
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
                        GameScreen(
                            modifier = Modifier.fillMaxSize(),
                            uiState = uiState,
                            onIntent = viewModel::onIntent,
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
                Button(onClick = { viewModel.onIntent(RoomIntent.ClearError) }) {
                    Text("Dismiss")
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
    }
}

@Composable
private fun GameScreen(
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
            uiState.gameState?.handNumber
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
