package com.aaronchancey.poker.config

import com.aaronchancey.poker.BuildConfig

actual object AppConfig {
    actual val isProduction: Boolean = !BuildConfig.DEBUG

    private val serverHost: String get() = BuildConfig.SERVER_HOST

    actual val baseUrl: String
        get() = if (isProduction) {
            "https://poker-server-824217504292.us-east1.run.app"
        } else {
            "http://$serverHost:8080"
        }

    actual val wsHost: String
        get() = if (isProduction) "poker-server-824217504292.us-east1.run.app" else serverHost

    actual val wsPort: Int
        get() = if (isProduction) 443 else 8080
}
