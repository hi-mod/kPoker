package com.aaronchancey.poker.di

import com.aaronchancey.poker.network.PokerRepository
import com.aaronchancey.poker.network.PokerWebSocketClient
import com.aaronchancey.poker.network.RoomClient
import com.aaronchancey.poker.presentation.lobby.LobbyViewModel
import com.aaronchancey.poker.presentation.room.ActionEvProvider
import com.aaronchancey.poker.presentation.room.HandDescriptionProvider
import com.aaronchancey.poker.presentation.room.RoomParams
import com.aaronchancey.poker.presentation.room.RoomViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
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

    single { RoomClient(get()) }
    // Factory scope: each RoomViewModel gets its own WebSocket connection and state
    factory { PokerWebSocketClient(get()) }
    factory { PokerRepository(get()) }
    factory { HandDescriptionProvider() }
    factory { ActionEvProvider() }

    // Singleton: LobbyViewModel lives for app lifetime and is accessed outside Window scope
    singleOf(::LobbyViewModel)

    // RoomViewModel requires RoomParams passed via parametersOf()
    viewModel { (params: RoomParams) ->
        RoomViewModel(
            params = params,
            settings = get(),
            repository = get(),
            handDescriptionProvider = get(),
            actionEvProvider = get(),
        )
    }
}
