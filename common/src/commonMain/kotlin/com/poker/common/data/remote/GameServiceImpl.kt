package com.poker.common.data.remote

import com.poker.common.data.TokenService
import com.poker.common.data.remote.dto.game.GameDto
import com.poker.common.data.remote.dto.poker.TableDto
import com.poker.common.domain.GameService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.flow

class GameServiceImpl(
    private val client: HttpClient,
    private val tokenService: TokenService,
) : GameService {
    override suspend fun getGames(): List<GameDto> = client.get {
        url(HttpRoutes.GAMES)
        header(HttpHeaders.Authorization, "Bearer ${tokenService.token}")
    }.body() ?: emptyList()

    override suspend fun startGame(gameId: String) = flow {
        val session = client.webSocketSession {
            url("${HttpRoutes.START_GAME}/$gameId")
            header(HttpHeaders.Authorization, "Bearer ${tokenService.token}")
        }
        session.send(Frame.Text("startGame"))
        while (true) {
            val game = session.receiveDeserialized<TableDto>()
            println(game)
            emit(game)
        }
    }
}
