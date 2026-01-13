package com.aaronchancey.poker.config

import kotlinx.browser.window

actual object AppConfig {
    actual val isProduction: Boolean
        get() = !window.location.hostname.let {
            it == "localhost" || it == "127.0.0.1" || it.startsWith("192.168.") || it.startsWith("10.")
        }

    actual val baseUrl: String
        get() = if (isProduction) {
            "${window.location.protocol}//${window.location.host}"
        } else {
            "http://localhost:8080"
        }

    actual val wsHost: String
        get() = if (isProduction) window.location.hostname else "localhost"

    actual val wsPort: Int
        get() = if (isProduction) {
            window.location.port.toIntOrNull() ?: if (window.location.protocol == "https:") 443 else 80
        } else {
            8080
        }
}
