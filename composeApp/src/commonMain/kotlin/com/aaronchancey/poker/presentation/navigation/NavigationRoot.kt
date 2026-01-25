package com.aaronchancey.poker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.presentation.lobby.LobbyIntent
import com.aaronchancey.poker.presentation.lobby.LobbyScreen
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.RoomViewModel
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.viewmodel.koinViewModel

sealed class Navigation {
    @Serializable
    data object Lobby : Navigation(), NavKey

    @Serializable
    data class Room(val roomId: String, val playerName: String, val playerId: PlayerId) :
        Navigation(),
        NavKey
}

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    settings: Settings,
) {
    val backStack = rememberNavBackStack(config, Navigation.Lobby)
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
            // rememberSceneSetupNavEntryDecorator(),
        ),
        entryProvider = { key ->
            when (key) {
                Navigation.Lobby -> {
                    NavEntry(key) {
                        val viewModel = koinViewModel<LobbyViewModel>()
                        val state by viewModel.state.collectAsStateWithLifecycle()
                        println("$key -> $state")
                        LobbyScreen(
                            state = state,
                            onIntent = { intent ->
                                viewModel.onIntent(intent)
                                // Also navigate when joining a room (single-window navigation mode)
                                if (intent is LobbyIntent.JoinRoom) {
                                    val request = state.openRooms.find { it.roomId == intent.roomId }
                                    if (request != null) {
                                        backStack.add(Navigation.Room(request.roomId, request.playerName, request.playerId))
                                    }
                                }
                            },
                        )
                    }
                }

                is Navigation.Room -> {
                    NavEntry(key) { key ->
                        val navKey = key as Navigation.Room
                        val viewModel = koinViewModel<RoomViewModel>()
                        val state by viewModel.uiState.collectAsStateWithLifecycle()
                    }
                }

                else -> throw RuntimeException("Invalid NavKey")
            }
        },
    )
}

// Creates the required serializing configuration for open polymorphism
private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Navigation.Lobby::class, Navigation.Lobby.serializer())
        }
    }
}
