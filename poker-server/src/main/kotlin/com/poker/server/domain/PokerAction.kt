package com.poker.server.domain

import kotlinx.serialization.Serializable

@Serializable
enum class PokerAction { Bet, Call, Check, Fold, Raise }