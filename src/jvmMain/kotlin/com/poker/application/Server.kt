package com.poker.application

import com.poker.application.plugins.configureHTTP
import com.poker.application.plugins.configureMonitoring
import com.poker.application.plugins.configureRouting
import com.poker.application.plugins.configureSerialization
import com.poker.application.plugins.configureSockets
import com.poker.application.routes.registerGameRoutes
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
    // configureSecurity()
    configureRouting()

    registerGameRoutes()
}
