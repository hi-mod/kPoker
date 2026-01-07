package com.aaronchancey.poker.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import org.slf4j.event.Level

fun Application.configureMonitoring() = install(CallLogging) {
    level = Level.DEBUG
    // filter { call -> call.request.path().startsWith("/") }
}
