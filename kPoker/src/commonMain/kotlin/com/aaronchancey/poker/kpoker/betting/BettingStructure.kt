package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import kotlinx.serialization.Serializable

@Serializable
data class BettingStructure(
    val bettingType: BettingType,
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val ante: ChipAmount = 0.0,
    val bringIn: ChipAmount = 0.0,
    val minBet: ChipAmount = bigBlind,
    val maxBet: ChipAmount? = null, // null = no limit
    val maxRaises: Int? = null, // null = unlimited
    val minDenomination: ChipAmount = 1.0,
) {
    companion object {
        fun noLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            ante: ChipAmount = 0.0,
            minDenomination: ChipAmount = 1.0,
        ) = BettingStructure(
            bettingType = BettingType.NO_LIMIT,
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
            bettingType = BettingType.POT_LIMIT,
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
            bettingType = BettingType.LIMIT,
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

enum class BettingType {
    NO_LIMIT,
    POT_LIMIT,
    LIMIT,
}
