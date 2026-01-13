package com.aaronchancey.poker.config

actual object AppConfig {
    // Check system property or env var, default to dev
    actual val isProduction: Boolean
        get() = System.getProperty("poker.production")?.toBoolean()
            ?: System.getenv("POKER_PRODUCTION")?.toBoolean()
            ?: false

    actual val baseUrl: String
        get() = System.getProperty("poker.baseUrl")
            ?: System.getenv("POKER_BASE_URL")
            ?: if (isProduction) "https://poker-server-824217504292.us-east1.run.app" else "http://localhost:8080"

    actual val wsHost: String
        get() = System.getProperty("poker.wsHost")
            ?: System.getenv("POKER_WS_HOST")
            ?: if (isProduction) "poker-server-824217504292.us-east1.run.app" else "localhost"

    actual val wsPort: Int
        get() = System.getProperty("poker.wsPort")?.toIntOrNull()
            ?: System.getenv("POKER_WS_PORT")?.toIntOrNull()
            ?: if (isProduction) 443 else 8080
}
