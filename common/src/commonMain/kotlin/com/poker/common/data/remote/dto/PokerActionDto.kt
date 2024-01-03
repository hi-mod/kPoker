package com.poker.common.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
enum class PokerActionDto { Bet, Call, Check, Fold, Raise }