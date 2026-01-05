package com.aaronchancey.poker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
