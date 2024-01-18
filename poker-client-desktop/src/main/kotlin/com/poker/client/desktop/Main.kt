package com.poker.client.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poker.client.desktop.presentation.poker.ShowPlayers
import com.poker.common.data.remote.dto.PlayerDto
import com.poker.common.di.CommonModuleImpl
import kotlinx.coroutines.launch
import java.awt.MenuBar

fun main() = application {
    val windowState = rememberWindowState()

    val commonModule =
        remember {
            CommonModuleImpl()
        }
    // Remember a CoroutineScope for the current Composable
    val scope = rememberCoroutineScope()

    var players by remember { mutableStateOf(emptyList<PlayerDto>()) }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Poker Desktop Client",
    ) {
        MenuBar {
            Menu(
                text = "Poker",
            ) {
                Item(
                    text = "Start Game",
                    onClick = {
                        scope.launch {
                            commonModule.pokerService.startGame().collect { game ->
                                players = game.players
                            }
                        }
                    },
                )
                Separator()
                Item(
                    text = "Quit",
                    onClick = ::exitApplication,
                )
            }
        }
        Column {
            ShowPlayers(players = players)
        }
    }

    LaunchedEffect(Unit) {
        commonModule.pokerService.startGame().collect { game ->
            players = game.players
        }
    }
}
