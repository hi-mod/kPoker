package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import kotlinx.serialization.Serializable

@Serializable
data class BettingStructure(
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val ante: ChipAmount = 0.0,
    val bringIn: ChipAmount = 0.0,
    val minBet: ChipAmount = bigBlind,
    val maxBet: ChipAmount? = null, // null = no limit
    val maxRaises: Int? = null, // null = unlimited
    val minDenomination: ChipAmount = 1.0,
) {
    val isNoLimit: Boolean get() = maxBet == null
    val isFixedLimit: Boolean get() = maxBet != null && maxRaises != null

    companion object {
        fun noLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            minDenomination: ChipAmount = 1.0,
        ) = BettingStructure(
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            minDenomination = minDenomination,
        )

        fun potLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            minDenomination: ChipAmount = 1.0,
        ) = BettingStructure(
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            minDenomination = minDenomination,
        ) // Pot limit enforced in game logic

        fun fixedLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            maxRaises: Int = 4,
            minDenomination: ChipAmount = 1.0,
        ) = BettingStructure(
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            ante = ante,
            minBet = bigBlind,
            maxBet = bigBlind,
            maxRaises = maxRaises,
            minDenomination = minDenomination,
        )
    }
}
