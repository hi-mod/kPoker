package com.aaronchancey.poker.presentation.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Standalone lobby composable for the multi-window architecture.
 *
 * This is the main entry point for the lobby window. It handles:
 * - Displaying the room list
 * - Triggering room joins via intents
 *
 * Open rooms are managed via [LobbyState.openRooms] and rendered by the platform layer.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LobbyScreen(
    state: LobbyState,
    onIntent: (LobbyIntent) -> Unit,
) = MaterialExpressiveTheme {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
    ) {
        Lobby(
            state = state,
            onJoinRoom = { roomInfo, playerName ->
                onIntent(
                    LobbyIntent.JoinRoom(
                        roomId = roomInfo.roomId,
                        roomName = roomInfo.roomName,
                        playerName = playerName,
                    ),
                )
            },
            onRefresh = { onIntent(LobbyIntent.RefreshRooms) },
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
