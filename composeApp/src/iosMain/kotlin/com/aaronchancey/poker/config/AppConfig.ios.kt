package com.aaronchancey.poker.config

import platform.Foundation.NSBundle

actual object AppConfig {
    // Check if running in DEBUG configuration via Info.plist or default to false
    actual val isProduction: Boolean
        get() = NSBundle.mainBundle.infoDictionary?.get("IS_PRODUCTION") as? Boolean ?: true

    actual val baseUrl: String
        get() = if (isProduction) {
            "https://poker-server-824217504292.us-east1.run.app"
        } else {
            "http://localhost:8080"
        }

    actual val wsHost: String
        get() = if (isProduction) "poker-server-824217504292.us-east1.run.app" else "localhost"

    actual val wsPort: Int
        get() = if (isProduction) 443 else 8080
}
