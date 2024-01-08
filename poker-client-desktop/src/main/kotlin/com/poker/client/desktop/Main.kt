package com.poker.client.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poker.client.desktop.presentation.poker.ShowPlayers
import com.poker.common.data.remote.dto.PlayerDto
import com.poker.common.di.CommonModuleImpl
import kotlinx.coroutines.launch

fun main() =
    application {
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
            Column {
                Text("Hello, world!")
                Button(
                    onClick = {
                        scope.launch {
                            commonModule.pokerService.startGame().collect { game ->
                                players = game.players
                            }
                        }
                    },
                ) {
                    Text("Start Game")
                }

                ShowPlayers(players = players)
            }
        }
    }
