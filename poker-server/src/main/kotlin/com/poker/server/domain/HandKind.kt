package com.poker.server.domain

import kotlinx.serialization.Serializable

@Serializable
enum class HandKind(val rank: Int, val description: String) {
    RoyalFlush(0x9EDCBA, "Royal Flush"),
    StraightFlush(0x900000, "Straight Flush"),
    FourOfAKind(0x800000, "Four of a Kind"),
    FullHouse(0x700000, "Full House"),
    Flush(0x600000, "Flush"),
    Straight(0x500000, "Straight"),
    ThreeOfAKind(0x400000, "Three of a Kind"),
    TwoPair(0x300000, "Two Pair"),
    Pair(0x200000, "Pair"),
    HighCard(0x100000, "High Card"),
    Error(-1, "error"),
}
