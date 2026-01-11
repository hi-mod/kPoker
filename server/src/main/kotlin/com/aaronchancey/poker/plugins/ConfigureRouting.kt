package com.aaronchancey.poker.plugins

import com.aaronchancey.poker.routes.routeGameSocket
import com.aaronchancey.poker.routes.routeRooms
import io.ktor.server.application.Application
import io.ktor.server.http.content.staticFiles
import io.ktor.server.http.content.staticResources
import io.ktor.server.routing.routing
import java.io.File

fun Application.configureRouting() {
    routing {
        routeRooms()
        routeGameSocket()

        val staticDir = File("static")
        if (staticDir.exists() && staticDir.isDirectory) {
            staticFiles("/", staticDir) {
                default("index.html")
            }
        } else {
            staticResources("/", "static") {
                default("index.html")
            }
        }
    }
}
