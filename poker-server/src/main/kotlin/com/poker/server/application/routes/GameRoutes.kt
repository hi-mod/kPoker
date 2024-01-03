package com.poker.server.application.routes

import com.poker.common.data.mappers.toGameDto
import com.poker.common.domain.Game
import com.poker.common.domain.GameEvent
import com.poker.common.domain.GameState
import com.poker.common.domain.Player
import com.poker.server.statemachine.GameStateMachine
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

fun Application.registerGameRoutes() = routing {
    gameRouting()
}

@OptIn(ExperimentalSerializationApi::class)
fun Route.gameRouting() = webSocket("/game") {
    for (frame in incoming) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            when (text) {
                "startGame" -> {
                    val gameEvents = Channel<GameEvent>()
                    launch {
                        val gameStateMachine = GameStateMachine()
                        outgoing.send(Frame.Text("Game started"))
                        gameStateMachine.stateMachine(gameEvents)
                            .filter { it !is GameState.Idle }
                            .collect { gameState ->
                                sendSerialized(gameState.toGameDto())
                            }
                    }
                    launch {
                        gameEvents.send(GameEvent.StartGame(Game()))
                        gameEvents.send(GameEvent.AddPlayer(Player("1", "Player 1")))
                        gameEvents.send(GameEvent.AddPlayer(Player("2", "Player 2")))
                        gameEvents.send(GameEvent.ChooseStartingDealer)
                        gameEvents.send(GameEvent.SetButton)
                        gameEvents.send(GameEvent.GameReady)
                        // gameEvents.send(GameEvent.StartHand)
                    }
                }
                "endGame" -> {
                    outgoing.send(Frame.Text("Game ended"))
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }
}
