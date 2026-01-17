package com.aaronchancey.poker.kpoker.core

import kotlinx.serialization.Serializable

/**
 * Indicates whether a card is visible to all players or only to its owner.
 *
 * Used primarily for Stud poker variants where some cards are dealt face-up (PUBLIC)
 * and others face-down (PRIVATE). In Hold'em and Omaha, all hole cards are PRIVATE.
 */
@Serializable
enum class CardVisibility {
    /** Face-down card, visible only to the card's owner. */
    PRIVATE,

    /** Face-up card, visible to all players at the table. */
    PUBLIC,
}
