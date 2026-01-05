package com.aaronchancey.poker.routes

import com.aaronchancey.poker.room.RoomManager
import com.aaronchancey.poker.ws.ConnectionManager
import io.ktor.util.AttributeKey

val RoomManagerKey = AttributeKey<RoomManager>("RoomManager")
val ConnectionManagerKey = AttributeKey<ConnectionManager>("ConnectionManager")
