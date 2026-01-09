package com.aaronchancey.poker.plugins

import com.aaronchancey.poker.routes.routeGameSocket
import com.aaronchancey.poker.routes.routeRooms
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        routeRooms()
        routeGameSocket()
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
