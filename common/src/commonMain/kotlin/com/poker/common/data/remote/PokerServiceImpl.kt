package com.poker.common.data.remote

import com.poker.common.data.TokenService
import com.poker.common.data.remote.dto.poker.GameDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import kotlinx.coroutines.flow.flow

class PokerServiceImpl(
    private val client: HttpClient,
    private val tokenService: TokenService,
) : PokerService {
    override suspend fun startGame(gameId: String) = flow {
        val session = client.webSocketSession {
            url("${HttpRoutes.START_GAME}/$gameId")
            header(HttpHeaders.Authorization, "Bearer ${tokenService.token}")
        }
        session.send(Frame.Text("startGame"))
        while (true) {
            val game = session.receiveDeserialized<GameDto>()
            println(game)
            emit(game)
        }
    }
}
