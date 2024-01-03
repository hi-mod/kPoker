package com.poker.server.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GameType { RingGame, Tournament, SitNGo }
