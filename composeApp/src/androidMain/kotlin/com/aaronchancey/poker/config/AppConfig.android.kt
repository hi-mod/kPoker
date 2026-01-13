package com.aaronchancey.poker.config

import com.aaronchancey.poker.BuildConfig

actual object AppConfig {
    actual val isProduction: Boolean = !BuildConfig.DEBUG

    actual val baseUrl: String
        get() = if (isProduction) {
            "https://poker-server-824217504292.us-east1.run.app"
        } else {
            "http://10.0.2.2:8080" // Android emulator localhost
        }

    actual val wsHost: String
        get() = if (isProduction) "poker-server-824217504292.us-east1.run.app" else "10.0.2.2"

    actual val wsPort: Int
        get() = if (isProduction) 443 else 8080
}
