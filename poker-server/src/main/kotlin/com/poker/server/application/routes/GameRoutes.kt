package com.poker.server.application.routes

import com.poker.common.data.mappers.toGameDto
import com.poker.common.data.remote.dto.game.GameDto
import com.poker.common.data.remote.dto.poker.LevelDto
import com.poker.common.domain.GameEvent
import com.poker.common.domain.GameState
import com.poker.common.domain.Level
import com.poker.common.domain.Player
import com.poker.common.domain.Table
import com.poker.common.statemachine.GameStateMachine
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.Collections
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

fun Application.registerGameRoutes() = routing {
    authenticate("auth-jwt") {
        gameManagement()
        gameRoute()
    }
}

data class GameData
@OptIn(ExperimentalSerializationApi::class)
constructor(
    val gameStateMachine: GameStateMachine,
    val gameEvents: Channel<GameEvent>,
    val connections: MutableSet<Connection>,
)

data class Connection(
    val session: DefaultWebSocketSession,
    val username: String,
)

private fun Route.gameManagement() {
    get("/games") {
        call.respond(
            listOf(
                GameDto(
                    id = UUID(0, 0),
                    name = "Test Game",
                    description = "A test game",
                    numPlayers = 0,
                    level = LevelDto(
                        smallBlind = 5.0,
                        bigBlind = 10.0,
                    ),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun Route.gameRoute() {
    val games = Collections.synchronizedMap<String, GameData?>(LinkedHashMap())
    webSocket("/game/{gameId}") {
        val gameId = call.parameters["gameId"]
        if (gameId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No game ID provided"))
            return@webSocket
        }
        for (frame in incoming) {
            if (frame !is Frame.Text) continue
            val text = frame.readText()
            when (text) {
                "startGame" -> {
                    val principal = call.principal<UserIdPrincipal>()
                    if (principal == null) {
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No user"))
                        return@webSocket
                    }
                    val gameData = games.getOrPut(
                        key = gameId,
                        defaultValue = {
                            GameData(
                                gameStateMachine = GameStateMachine(),
                                gameEvents = Channel(),
                                connections = Collections.synchronizedSet(LinkedHashSet()),
                            )
                        },
                    )
                    if (gameData.connections.any { it.username == principal.name }) {
                        println("User already in game")
                        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No user"))
                        return@webSocket
                    }
                    println("Adding user!")
                    val thisConnection = Connection(this, principal.name)
                    gameData.connections += thisConnection
                    sendGameUpdates(gameData)
                    startPokerGame(gameData.gameEvents)
                }

                "endGame" -> {
                    outgoing.send(Frame.Text("Game ended"))
                    close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                }
            }
        }
    }
}

private fun DefaultWebSocketServerSession.startPokerGame(gameEvents: Channel<GameEvent>) {
    launch {
        gameEvents.send(
            GameEvent.StartGame(
                table = Table(
                    level = Level(
                        smallBlind = 5.0,
                        bigBlind = 10.0,
                    ),
                ),
            ),
        )
        val principal = call.principal<UserIdPrincipal>()
        val username = principal?.name
        username?.let {
            gameEvents.send(
                GameEvent.AddPlayer(
                    Player(
                        id = username,
                        name = username,
                        chips = 1000.0,
                    ),
                ),
            )
        }
        /*
                            gameEvents.send(GameEvent.ChooseStartingDealer)
                            gameEvents.send(GameEvent.SetButton)
                            gameEvents.send(GameEvent.GameReady)
                            gameEvents.send(GameEvent.StartHand)
*/
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun DefaultWebSocketServerSession.sendGameUpdates(gameData: GameData) {
    launch {
        gameData.gameStateMachine.stateMachine(gameData.gameEvents)
            .filter { it !is GameState.Idle }
            .collect { gameState ->
                gameData.connections.forEach {
                    (it.session as WebSocketServerSession).sendSerialized(gameState.toGameDto())
                }
            }
    }
}
