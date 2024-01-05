package com.poker.common.data.remote

import com.poker.common.data.remote.dto.GameDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

class PokerServiceImpl(
    private val client: HttpClient,
) : PokerService {
    override suspend fun startGame() {
        client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/game") {
            this.send(Frame.Text("startGame"))
            while(true) {
                val game = receiveDeserialized<GameDto>()
                println(game)
            }
        }
    }
}