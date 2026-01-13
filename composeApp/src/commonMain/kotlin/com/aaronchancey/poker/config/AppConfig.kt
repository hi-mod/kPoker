package com.aaronchancey.poker.config

expect object AppConfig {
    val baseUrl: String
    val wsHost: String
    val wsPort: Int
    val isProduction: Boolean
}
