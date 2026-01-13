package com.aaronchancey.poker.di

import com.aaronchancey.poker.network.RoomClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AppModule {
    val httpClient by lazy {
        HttpClient {
            install(WebSockets) {
                contentConverter =
                    KotlinxWebsocketSerializationConverter(
                        Json {
                            ignoreUnknownKeys = true
                            encodeDefaults = true
                        },
                    )
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    },
                )
            }
        }
    }

    val roomClient by lazy {
        RoomClient(httpClient)
    }
}
