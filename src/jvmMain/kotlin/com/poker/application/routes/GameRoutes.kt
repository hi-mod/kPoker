package com.poker.application.routes

import com.poker.statemachine.GameStateMachine
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText

fun Application.registerGameRoutes() = routing {
    gameRouting()
}

fun Route.gameRouting() = webSocket("/game") {
    for (frame in incoming) {
        if (frame is Frame.Text) {
            val text = frame.readText()
            when (text) {
                "startGame" -> {
                    outgoing.send(Frame.Text("Game started"))
                }
                "endGame" -> {
                    outgoing.send(Frame.Text("Game ended"))
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }
}

