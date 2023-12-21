package com.poker.application.routes

import com.poker.domain.Game
import com.poker.domain.Player
import com.poker.statemachine.GameEvent
import com.poker.statemachine.GameState
import com.poker.statemachine.GameStateMachine
import com.poker.statemachine.IGameState
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
                        gameStateMachine.stateMachine(gameEvents).collect { gameState ->
                            when (gameState) {
                                is GameState.GameStart -> {
                                    outgoing.send(Frame.Text("Game started"))
                                    sendSerialized(gameStateMachine.gameState)
                                    sendSerialized((gameStateMachine.gameState as IGameState).game)
                                }
                                else -> {}
                            }
                        }
                    }
                    launch {
                        gameEvents.send(GameEvent.StartGame(Game()))
                        gameEvents.send(GameEvent.AddPlayer(Player("1", "Player 1")))
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

