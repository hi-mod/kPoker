package com.poker.client.desktop.di

import com.poker.client.desktop.table.presentation.PokerViewModel
import com.poker.common.data.TokenService
import com.poker.common.data.remote.GameServiceImpl
import com.poker.common.data.remote.LoginServiceImpl
import com.poker.common.data.remote.dto.game.UUIDSerializer
import com.poker.common.domain.AppSettings
import com.poker.common.domain.GameDataSource
import com.russhwolf.settings.PreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import java.time.Duration
import java.util.prefs.Preferences
import kotlinx.coroutines.MainScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

interface AppModule {
    val pokerViewModel: PokerViewModel
}

class AppModuleImpl : AppModule {
    private val ktorClient by lazy {
        HttpClient(CIO) {
            install(Auth) {
                bearer {
                    loadTokens {
                        tokenService.token?.let {
                            BearerTokens(
                                accessToken = it,
                                refreshToken = it,
                            )
                        }
                    }
                }
            }
            install(WebSockets) {
                pingInterval = Duration.ofSeconds(15).toMillis()
                maxFrameSize = Long.MAX_VALUE
                contentConverter =
                    KotlinxWebsocketSerializationConverter(
                        Json {
                            encodeDefaults = true
                        },
                    )
            }
            // Install the ContentNegotiation feature and configure it to use JSON.
            install(ContentNegotiation) {
                json(
                    json = Json {
                        serializersModule = SerializersModule {
                            contextual(UUIDSerializer)
                        }
                        isLenient = true
                        ignoreUnknownKeys = true
                        allowSpecialFloatingPointValues = true
                        useArrayPolymorphism = true
                        encodeDefaults = true
                    },
                )
            }
            // Install the Logging feature and configure it to log all requests and responses.
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
                // sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
    }

    private val loginService by lazy {
        LoginServiceImpl(client = ktorClient)
    }

    private val gameService by lazy {
        GameServiceImpl(
            client = ktorClient,
            tokenService = tokenService,
        )
    }

    private val tokenService by lazy {
        TokenService()
    }

    override val pokerViewModel by lazy {
        PokerViewModel(
            coroutineScope = MainScope(),
            gameDataSource = gameDataSource,
            loginService = loginService,
            tokenService = tokenService,
            appSettings = appSettings,
        )
    }

    private val settings by lazy {
        PreferencesSettings(Preferences.userRoot())
    }

    private val appSettings by lazy {
        AppSettings(settings)
    }

    private val gameDataSource by lazy {
        GameDataSource(gameService = gameService)
    }
}
