package com.poker.common.data.remote

data object HttpRoutes {
    private const val BASE_URL = "http://127.0.0.1:8080"
    private const val WS_BASE_URL = "ws://127.0.0.1:8080"
    const val START_GAME = "$WS_BASE_URL/game"
    const val LOGIN = "$BASE_URL/login"
}
