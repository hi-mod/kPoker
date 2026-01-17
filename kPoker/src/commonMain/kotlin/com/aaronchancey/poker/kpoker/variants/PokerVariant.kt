package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.core.CardVisibility

interface PokerVariant {
    val name: String
    val holeCardCount: Int
    val usesCommunityCards: Boolean
    val minPlayers: Int
    val maxPlayers: Int
    val description: String

    /**
     * Visibility pattern for dealt hole cards.
     *
     * Each element corresponds to a card position in the deal order:
     * - [CardVisibility.PRIVATE]: Face-down, only visible to the card owner
     * - [CardVisibility.PUBLIC]: Face-up, visible to all players
     *
     * Default is all cards face-down (suitable for Hold'em, Omaha).
     * Stud variants override this with mixed patterns like [PRIVATE, PRIVATE, PUBLIC].
     */
    val dealingPattern: List<CardVisibility>
        get() = List(holeCardCount) { CardVisibility.PRIVATE }
}

data class VariantConfig(
    val variant: PokerVariant,
    val bettingStructure: BettingStructure,
)
