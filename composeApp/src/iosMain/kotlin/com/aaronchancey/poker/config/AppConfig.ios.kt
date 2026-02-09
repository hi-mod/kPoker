package com.aaronchancey.poker.config

import platform.Foundation.NSBundle

actual object AppConfig {
    // Check if running in DEBUG configuration via Info.plist or default to false
    actual val isProduction: Boolean
        get() = NSBundle.mainBundle.infoDictionary?.get("IS_PRODUCTION") as? Boolean ?: true

    private val serverHost: String
        get() = (NSBundle.mainBundle.infoDictionary?.get("SERVER_HOST") as? String) ?: "localhost"

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
