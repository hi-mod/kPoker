package com.aaronchancey.poker.kpoker.formats

import com.aaronchancey.poker.kpoker.player.ChipAmount

enum class GameFormat {
    CASH_GAME,
    SIT_AND_GO,
    MULTI_TABLE_TOURNAMENT,
}

interface GameFormatConfig {
    val format: GameFormat
    val minPlayers: Int
    val maxPlayers: Int
    val minDenomination: ChipAmount
}

data class CashGameConfig(
    override val minDenomination: ChipAmount,
    override val minPlayers: Int = 2,
    override val maxPlayers: Int = 9,
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val ante: ChipAmount = 0.0,
    val minBuyIn: ChipAmount, // Usually 20-50 big blinds
    val maxBuyIn: ChipAmount, // Usually 100-200 big blinds
    val allowRebuy: Boolean = true,
    val allowTopUp: Boolean = true,
    val rakePercent: Double = 0.0,
    val rakeCap: ChipAmount = 0.0,
) : GameFormatConfig {
    override val format = GameFormat.CASH_GAME
}
