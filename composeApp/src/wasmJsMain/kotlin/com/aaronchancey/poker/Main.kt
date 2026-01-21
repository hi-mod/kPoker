package com.aaronchancey.poker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronchancey.poker.di.appModule
import com.aaronchancey.poker.presentation.lobby.LobbyIntent
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.RoomParams
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.aaronchancey.poker.window.RoomWindowRequest
import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinConfiguration
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
 * - Lobby polls popup windows to detect when they close
 * - Room window also notifies lobby via postMessage (backup mechanism)
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize JS popup tracking
    initPopupTracking()

    val settings = StorageSettings()
    val urlParams = parseUrlParams()

    // Diagnostic logging: URL params
    console.log("[Main] Parsed URL params: roomId=${urlParams.roomId}, playerName=${urlParams.playerName}, playerId=${urlParams.playerId}")
    console.log("[Main] Mode: ${if (urlParams.roomId != null) "ROOM" else "LOBBY"}")

    ComposeViewport {
        KoinApplication(
            configuration = koinConfiguration(
                declaration = {
                    modules(
                        appModule,
                        module {
                            single<Settings> { settings }
                        },
                    )
                },
            ),
            content = {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .safeContentPadding()
                        .fillMaxSize(),
                ) {
                    if (urlParams.roomId != null) {
                        // Room mode - opened from lobby popup or direct link
                        console.log("[Main] Room mode: Initializing for roomId=${urlParams.roomId}")
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
                                console.log("[Main] Room disconnected, notifying opener and closing window")
                                // Notify opener (lobby) that room closed (backup mechanism)
                                notifyOpenerRoomClosed(urlParams.roomId)
                                // Close this window/tab
                                window.close()
                            },
                            onIntent = viewModel::onIntent,
                        )
                    } else {
                        // Lobby mode
                        console.log("[Main] Lobby mode: Initializing")
                        val lobbyViewModel = koinViewModel<LobbyViewModel>()
                        val lobbyState by lobbyViewModel.state.collectAsStateWithLifecycle()

                        // Track which rooms we've opened (to avoid re-opening)
                        val openedRoomIds = remember { mutableStateOf(setOf<String>()) }

                        // Poll for closed windows and dispatch CloseRoom intents
                        LaunchedEffect(Unit) {
                            console.log("[Main] Lobby: Starting popup window polling")
                            while (isActive) {
                                delay(1000) // Check every second
                                val closedRoomId = getAndRemoveClosedPopup()
                                if (closedRoomId != null) {
                                    console.log("[Main] Lobby: Detected popup closed for roomId=$closedRoomId")
                                    openedRoomIds.value -= closedRoomId
                                    lobbyViewModel.onIntent(LobbyIntent.CloseRoom(closedRoomId))
                                }
                            }
                        }

                        // Open popups for new rooms
                        for (request in lobbyState.openRooms) {
                            if (request.roomId !in openedRoomIds.value) {
                                console.log("[Main] Lobby: Opening popup for roomId=${request.roomId}")
                                val opened = openRoomPopup(request)
                                if (opened) {
                                    openedRoomIds.value += request.roomId
                                }
                            }
                        }

                        // Clean up tracking for rooms removed from state
                        val stateRoomIds = lobbyState.openRooms.map { it.roomId }.toSet()
                        val removedRooms = openedRoomIds.value - stateRoomIds
                        if (removedRooms.isNotEmpty()) {
                            removedRooms.forEach { roomId ->
                                console.log("[Main] Lobby: Cleaning up popup tracking for roomId=$roomId")
                                removePopupTracking(roomId)
                            }
                            openedRoomIds.value -= removedRooms
                        }

                        LobbyScreen(
                            state = lobbyState,
                            onIntent = lobbyViewModel::onIntent,
                        )
                    }
                }
            },
        )
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
 * Returns true if popup was opened successfully.
 */
private fun openRoomPopup(request: RoomWindowRequest): Boolean {
    val params = buildString {
        append("?roomId=")
        append(encodeURIComponent(request.roomId))
        append("&playerName=")
        append(encodeURIComponent(request.playerName))
        append("&playerId=")
        append(encodeURIComponent(request.playerId))
    }

    val url = "${window.location.origin}${window.location.pathname}$params"
    console.log("[Main] openRoomPopup: Opening URL=$url")

    return openPopupJs(request.roomId, url)
}

/**
 * Notifies the opener window (lobby) that the room has closed.
 * Sends a JSON message with the roomId (backup mechanism for when polling fails).
 */
private fun notifyOpenerRoomClosed(roomId: String) {
    try {
        val message = """{"type":"room_closed","roomId":"$roomId"}"""
        console.log("[Main] notifyOpenerRoomClosed: Sending message to opener: $message")
        postMessageToOpener(message)
    } catch (e: Exception) {
        console.log("[Main] notifyOpenerRoomClosed: Failed to notify opener: ${e.message}")
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

// Console logging for diagnostics - must be lowercase to match JS's console object
@Suppress("ktlint:standard:class-naming", "ClassName")
private external object console {
    fun log(message: String)
}

// Popup window tracking - all managed in JS to avoid type casting issues
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
() => {
    if (!window._pokerPopups) {
        window._pokerPopups = {};
    }
}
""",
)
private external fun initPopupTracking()

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
(roomId, url) => {
    try {
        const popup = window.open(
            url,
            'poker_room_' + roomId,
            'width=1200,height=800,menubar=no,toolbar=no,location=no,status=no'
        );
        if (popup) {
            window._pokerPopups[roomId] = popup;
            return true;
        } else {
            // Popup blocked, try new tab
            window.open(url, '_blank');
            return false;
        }
    } catch (e) {
        console.log('[Main] openPopupJs error:', e);
        return false;
    }
}
""",
)
private external fun openPopupJs(roomId: String, url: String): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
() => {
    if (!window._pokerPopups) return null;
    for (const roomId in window._pokerPopups) {
        const popup = window._pokerPopups[roomId];
        if (popup.closed) {
            delete window._pokerPopups[roomId];
            return roomId;
        }
    }
    return null;
}
""",
)
private external fun getAndRemoveClosedPopup(): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
(roomId) => {
    if (window._pokerPopups && window._pokerPopups[roomId]) {
        delete window._pokerPopups[roomId];
    }
}
""",
)
private external fun removePopupTracking(roomId: String)
