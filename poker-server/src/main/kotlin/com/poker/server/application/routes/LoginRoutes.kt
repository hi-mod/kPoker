package com.poker.server.application.routes

import com.poker.common.data.remote.dto.login.UserDto
import com.poker.server.application.plugins.JwtConfig
import com.poker.server.data.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

val users = listOf(
    User(
        id = "1",
        username = "Aaron",
        password = "test1",
    ),
    User(
        id = "2",
        username = "test2",
        password = "test2",
    ),
    User(
        id = "3",
        username = "test3",
        password = "test3",
    ),
)

fun Application.registerLoginRoutes() = routing {
    post("/login") {
        // Authenticate the user and generate a JWT token
        val userDto = call.receive<UserDto>()
        val user = users.firstOrNull { it.username == userDto.username && it.password == userDto.password }
        if (user == null) {
            return@post call.respondText("Missing or malformed gameId", status = HttpStatusCode.Unauthorized)
        }
        val token = JwtConfig.makeToken(user.username)
        call.respondText(token)
    }
}
