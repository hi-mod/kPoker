package com.aaronchancey.poker.kpoker.betting

import kotlinx.serialization.Serializable

@Serializable
enum class BettingRoundType {
    PRE_FLOP,
    FLOP,
    TURN,
    RIVER,
}
