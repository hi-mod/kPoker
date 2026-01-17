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
import androidx.compose.foundation.layout.requiredHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerStatus
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.presentation.game.GameEffect
import com.aaronchancey.poker.presentation.game.GameIntent
import com.aaronchancey.poker.presentation.game.GameUiState
import com.aaronchancey.poker.presentation.game.GameViewModel
import com.aaronchancey.poker.presentation.game.PlayerActions
import com.aaronchancey.poker.presentation.game.components.DealtCardView
import com.aaronchancey.poker.presentation.game.components.PlayingCard
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.presentation.room.RoomsScreen
import com.russhwolf.settings.Settings

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App(
    settings: Settings,
    viewModel: GameViewModel = viewModel { GameViewModel(settings) },
) = MaterialExpressiveTheme {
    val uiState by viewModel.uiState.collectAsState()

    // Handle side effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is GameEffect.ShowToast -> {
                    // Platform-specific toast implementation
                }

                is GameEffect.NavigateToLobby -> {
                    // Navigation handling
                }

                is GameEffect.PlaySound -> {
                    // Sound playback
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
                val roomViewModel = viewModel { RoomViewModel() }
                val roomState by roomViewModel.state.collectAsState()
                RoomsScreen(
                    state = roomState,
                    onIntent = viewModel::onIntent,
                )
            }

            ConnectionState.CONNECTING -> {
                CircularProgressIndicator()
                Text("Connecting...")
            }

            ConnectionState.CONNECTED, ConnectionState.RECONNECTING -> {
                if (uiState.roomInfo != null) {
                    GameScreen(
                        uiState = uiState,
                        onIntent = viewModel::onIntent,
                    )
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
            Button(onClick = { viewModel.onIntent(GameIntent.ClearError) }) {
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
private fun GameScreen(
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
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
            Text("Community Cards:")
            Row {
                state.communityCards.forEach { card ->
                    PlayingCard(
                        modifier = Modifier.requiredHeight(100.dp),
                        card = card,
                    )
                }
            }
            Text("Pot: ${state.totalPot}")

            Spacer(modifier = Modifier.height(16.dp))

            // Display seats
            Seats(state, uiState.gameState, uiState, onIntent)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onIntent(GameIntent.LeaveSeat) },
                enabled = !uiState.isLoading,
            ) {
                Text("Leave Seat")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onIntent(GameIntent.Disconnect) },
                enabled = !uiState.isLoading,
            ) {
                Text("Disconnect")
            }
        }
    }
}

@Composable
private fun Seats(
    state: GameState,
    gameState: GameState,
    uiState: GameUiState,
    onIntent: (GameIntent) -> Unit,
) {
    state.table.seats.forEach { seat ->
        val playerState = seat.playerState
        if (playerState != null) {
            val text = buildString {
                append("Seat ${seat.number}: ${playerState.player.name} - ")
                if (playerState.status == PlayerStatus.WAITING) {
                    append("(Waiting for next hand) ")
                }
                append("${playerState.chips} chips - Bet: ${playerState.currentBet} ")
                if (playerState.isDealer) {
                    append("(Dealer) ")
                }
                if (gameState.winners.find { it.playerId == playerState.player.id } != null) {
                    append("Winner!")
                }
                if (uiState.playerId == playerState.player.id) {
                    append(uiState.handDescription)
                }
            }
            Text(text)
            Row {
                playerState.dealtCards.forEach { dealtCard ->
                    DealtCardView(
                        modifier = Modifier.requiredHeight(100.dp),
                        dealtCard = dealtCard,
                    )
                }
            }
            PlayerActions(playerState, uiState, onIntent)
        } else {
            Button(
                onClick = { onIntent(GameIntent.TakeSeat(seat.number, 100.0)) },
                enabled = !uiState.isLoading,
            ) {
                Text("Take Seat ${seat.number}")
            }
        }
    }
}
