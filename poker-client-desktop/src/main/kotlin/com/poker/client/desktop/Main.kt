package com.poker.client.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poker.common.data.remote.dto.CardDto
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

                ShowPlayers(players)
            }
        }
    }

@Composable
private fun ShowPlayers(players: List<PlayerDto>) {
    players.forEach { player ->
        Row {
            player.hand?.forEach { card ->
                ShowCard(card)
            }
        }
        Text(player.hand.toString())
        Text(player.name)
    }
}

@Composable
private fun ShowCard(card: CardDto) {
    val cardDeck = painterResource("deck/${card.rank.cardName}${card.suit.shortName}.svg")
    Image(
        painter = cardDeck,
        contentDescription = "card",
    )
}
