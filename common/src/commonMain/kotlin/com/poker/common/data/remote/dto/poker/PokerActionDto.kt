package com.poker.common.data.remote.dto.poker

import kotlinx.serialization.Serializable

@Serializable
enum class PokerActionDto { Bet, Call, Check, Fold, Raise }
