package com.aaronchancey.poker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronchancey.poker.di.appModule
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.RoomParams
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.window.RoomWindowRequest
import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.window
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

/**
 * Web entry point with URL-based routing.
 *
 * URL patterns:
 * - No params: Lobby mode
 * - ?roomId=xxx&playerName=yyy&playerId=zzz: Room mode
 *
 * Multi-window strategy:
 * - Lobby opens room in a popup window with URL params
 * - Room window notifies lobby via postMessage when closing
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val settings = StorageSettings()
    val urlParams = parseUrlParams()

    ComposeViewport {
        KoinApplication(application = {
            modules(
                appModule,
                module {
                    single<Settings> { settings }
                },
            )
        }) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .safeContentPadding()
                    .fillMaxSize(),
            ) {
                if (urlParams.roomId != null) {
                    // Room mode - opened from lobby popup or direct link
                    val params = RoomParams(
                        roomId = urlParams.roomId,
                        playerName = urlParams.playerName ?: "Player",
                        playerId = urlParams.playerId ?: "",
                    )
                    val viewModel = koinViewModel<RoomViewModel>(
                        key = urlParams.roomId,
                        parameters = { parametersOf(params) },
                    )
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    RoomScreen(
                        uiState = uiState,
                        effects = viewModel.effects,
                        onDisconnected = {
                            // Notify opener (lobby) that room closed
                            notifyOpenerRoomClosed()
                            // Close this window/tab
                            window.close()
                        },
                        onIntent = viewModel::onIntent,
                    )
                } else {
                    // Lobby mode
                    val lobbyViewModel = koinViewModel<LobbyViewModel>()
                    val lobbyState by lobbyViewModel.state.collectAsStateWithLifecycle()

                    // Open popups for new rooms - track which we've opened
                    val openedRoomIds = androidx.compose.runtime.remember {
                        mutableSetOf<String>()
                    }
                    for (request in lobbyState.openRooms) {
                        if (request.roomId !in openedRoomIds) {
                            openedRoomIds.add(request.roomId)
                            openRoomPopup(request)
                        }
                    }

                    LobbyScreen(
                        state = lobbyState,
                        onIntent = lobbyViewModel::onIntent,
                    )
                }
            }
        }
    }
}

/**
 * Parsed URL parameters for room routing.
 */
private data class UrlParams(
    val roomId: String?,
    val playerName: String?,
    val playerId: String?,
)

/**
 * Parses URL query parameters.
 */
private fun parseUrlParams(): UrlParams {
    val search = window.location.search
    if (search.isBlank() || search == "?") {
        return UrlParams(null, null, null)
    }

    val params = search.removePrefix("?")
        .split("&")
        .associate { param ->
            val parts = param.split("=", limit = 2)
            val key = decodeURIComponent(parts[0])
            val value = if (parts.size > 1) decodeURIComponent(parts[1]) else ""
            key to value
        }

    return UrlParams(
        roomId = params["roomId"]?.ifBlank { null },
        playerName = params["playerName"]?.ifBlank { null },
        playerId = params["playerId"]?.ifBlank { null },
    )
}

/**
 * Opens a room in a popup window.
 */
private fun openRoomPopup(request: RoomWindowRequest) {
    val params = buildString {
        append("?roomId=")
        append(encodeURIComponent(request.roomId))
        append("&playerName=")
        append(encodeURIComponent(request.playerName))
        append("&playerId=")
        append(encodeURIComponent(request.playerId))
    }

    val url = "${window.location.origin}${window.location.pathname}$params"

    // Try to open as popup
    val popup = window.open(
        url = url,
        target = "poker_room_${request.roomId}",
        features = "width=1200,height=800,menubar=no,toolbar=no,location=no,status=no",
    )

    // Fallback to new tab if popup was blocked
    if (popup == null) {
        window.open(url, "_blank")
    }
}

/**
 * Notifies the opener window (lobby) that the room has closed.
 */
private fun notifyOpenerRoomClosed() {
    try {
        postMessageToOpener("room_closed")
    } catch (_: Exception) {
        // Opener may have been closed
    }
}

// JS interop functions
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(s) => encodeURIComponent(s)")
private external fun encodeURIComponent(s: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(s) => decodeURIComponent(s)")
private external fun decodeURIComponent(s: String): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(msg) => { if (window.opener) window.opener.postMessage(msg, '*'); }")
private external fun postMessageToOpener(message: String)
