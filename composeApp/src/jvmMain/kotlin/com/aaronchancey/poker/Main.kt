package com.aaronchancey.poker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronchancey.poker.di.appModule
import com.aaronchancey.poker.di.jvmModule
import com.aaronchancey.poker.presentation.lobby.LobbyIntent
import com.aaronchancey.poker.presentation.lobby.LobbyScreen
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.RoomParams
import com.aaronchancey.poker.presentation.room.RoomScreen
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.window.RoomWindowRequest
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinConfiguration

/**
 * Desktop entry point with multi-window architecture.
 *
 * - Lobby window is always open
 * - Room window opens when joining a room
 * - Room window closes when disconnecting
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(appModule, jvmModule)
            },
        ),
        content = {
            // LobbyViewModel hoisted to top level so room windows can route close events through it
            // Using getKoin().get() instead of koinViewModel because we're outside a Window
            // (koinViewModel requires ViewModelStoreOwner which Window provides)
            val koin = getKoin()
            val lobbyViewModel = remember { koin.get<LobbyViewModel>() }
            val lobbyState by lobbyViewModel.state.collectAsState()

            // Lobby window (always open)
            val lobbyWindowState = rememberWindowState()
            Window(
                onCloseRequest = ::exitApplication,
                state = lobbyWindowState,
                title = "Poker Lobby",
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .safeContentPadding()
                        .fillMaxSize(),
                ) {
                    LobbyScreen(
                        state = lobbyState,
                        onIntent = lobbyViewModel::onIntent,
                    )
                }
            }

            // Room windows - one per open room
            // key() at call site ensures Compose tracks each window by roomId across recompositions
            for (request in lobbyState.openRooms) {
                key(request.roomId) {
                    OpenRoom(
                        request = request,
                        onCloseRoom = { lobbyViewModel.onIntent(LobbyIntent.CloseRoom(request.roomId)) },
                    )
                }
            }
        },
    )
}

@Composable
private fun OpenRoom(
    request: RoomWindowRequest,
    onCloseRoom: (String) -> Unit,
) {
    val roomWindowState = rememberWindowState()
    Window(
        onCloseRequest = { onCloseRoom(request.roomId) },
        state = roomWindowState,
        title = "Room: ${request.roomName.ifBlank { request.roomId }}",
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            val params = RoomParams(
                roomId = request.roomId,
                playerName = request.playerName,
                playerId = request.playerId,
            )
            val viewModel = koinViewModel<RoomViewModel>(
                key = request.roomId,
                parameters = { parametersOf(params) },
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            RoomScreen(
                uiState = uiState,
                effects = viewModel.effects,
                onDisconnected = { onCloseRoom(request.roomId) },
                onIntent = viewModel::onIntent,
            )
        }
    }
}
