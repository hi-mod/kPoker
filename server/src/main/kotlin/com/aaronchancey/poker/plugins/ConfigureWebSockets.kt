package com.aaronchancey.poker.plugins

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(jsonConfig)
        pingPeriodMillis = 15.seconds.inWholeMilliseconds
        timeoutMillis = 30.seconds.inWholeMilliseconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
