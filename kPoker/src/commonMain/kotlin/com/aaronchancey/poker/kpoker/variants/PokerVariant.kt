package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure

interface PokerVariant {
    val name: String
    val holeCardCount: Int
    val usesCommunityCards: Boolean
    val minPlayers: Int
    val maxPlayers: Int
    val description: String
}

data class VariantConfig(
    val variant: PokerVariant,
    val bettingStructure: BettingStructure,
)
