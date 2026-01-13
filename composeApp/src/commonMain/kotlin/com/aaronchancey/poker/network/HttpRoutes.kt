package com.aaronchancey.poker.network

import com.aaronchancey.poker.config.AppConfig

object HttpRoutes {
    val ROOMS: String get() = "${AppConfig.baseUrl}/rooms"
}
