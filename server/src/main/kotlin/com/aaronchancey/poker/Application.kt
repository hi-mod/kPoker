package com.aaronchancey.poker

import com.aaronchancey.poker.plugins.configureHTTP
import com.aaronchancey.poker.plugins.configureMonitoring
import com.aaronchancey.poker.plugins.configureRouting
import com.aaronchancey.poker.plugins.configureSerialization
import com.aaronchancey.poker.plugins.configureWebSockets
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: SERVER_PORT
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureWebSockets()
    configureHTTP()
    configureRouting()
    configureMonitoring()
}
