package com.poker.server.application

import com.poker.server.application.plugins.configureHTTP
import com.poker.server.application.plugins.configureMonitoring
import com.poker.server.application.plugins.configureSecurity
import com.poker.server.application.plugins.configureSerialization
import com.poker.server.application.plugins.configureSockets
import com.poker.server.application.routes.registerGameRoutes
import com.poker.server.application.routes.registerLoginRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module,
    )
        .start(wait = true)
}

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()

    registerLoginRoutes()
    registerGameRoutes()
}
