package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import kotlinx.serialization.Serializable

@Serializable
data class BettingRound(
    val type: BettingRoundType,
    val currentBet: ChipAmount = 0.0,
    val minimumRaise: ChipAmount = 0.0,
    val lastRaiseAmount: ChipAmount = 0.0,
    val actingPlayerIndex: Int = 0,
    val lastAggressorId: PlayerId? = null,
    val actions: List<Action> = emptyList(),
    val isComplete: Boolean = false,
)
