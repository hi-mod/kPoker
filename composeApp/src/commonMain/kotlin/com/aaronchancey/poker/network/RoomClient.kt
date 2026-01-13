package com.aaronchancey.poker.network

import com.aaronchancey.poker.di.AppModule
import com.aaronchancey.poker.shared.message.RoomInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class RoomClient(
    private val client: HttpClient = AppModule.httpClient,
) {
    suspend fun getRooms() = safeCall<List<RoomInfo>> {
        client.get(HttpRoutes.ROOMS).body()
    }
}
