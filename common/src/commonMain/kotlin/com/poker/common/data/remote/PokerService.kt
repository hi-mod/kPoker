package com.poker.common.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.http.HttpHeaders

interface PokerService {
    suspend fun startGame()

    companion object {
        fun create(): PokerService = PokerServiceImpl(
            client = HttpClient {
                // Install the Auth feature and configure it to use bearer authentication.
                install(WebSockets) {
                    pingInterval = 20_000
                }
                // Install the ContentNegotiation feature and configure it to use JSON.
                install(ContentNegotiation) {
                    json(
                        json = Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
                // Install the Logging feature and configure it to log all requests and responses.
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.ALL
                    sanitizeHeader { header -> header == HttpHeaders.Authorization }
                }
            },
        )
    }
}
