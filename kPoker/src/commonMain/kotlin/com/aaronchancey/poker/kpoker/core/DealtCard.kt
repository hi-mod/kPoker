package com.aaronchancey.poker.kpoker.core

import kotlinx.serialization.Serializable

/**
 * Represents a card that has been dealt to a player, with associated visibility.
 *
 * The [card] property may be null when viewing another player's face-down cards,
 * indicating that a card exists in that position but its value is hidden from the viewer.
 *
 * @property card The actual card value, or null if hidden from the current viewer.
 * @property visibility Whether this card is face-up (PUBLIC) or face-down (PRIVATE).
 */
@Serializable
data class DealtCard(
    val card: Card?,
    val visibility: CardVisibility,
) {
    /** True if this card's value is known to the viewer. */
    val isVisible: Boolean get() = card != null

    companion object {
        /**
         * Creates a hidden card placeholder (used when filtering for opponents' views).
         * The card exists but its value is not revealed.
         */
        fun hidden() = DealtCard(card = null, visibility = CardVisibility.PRIVATE)

        /**
         * Wraps a list of cards as all face-down (private) dealt cards.
         * This is the default for Hold'em and Omaha hole cards.
         */
        fun allPrivate(cards: List<Card>) = cards.map {
            DealtCard(card = it, visibility = CardVisibility.PRIVATE)
        }

        /**
         * Wraps a list of cards as all face-up (public) dealt cards.
         */
        fun allPublic(cards: List<Card>) = cards.map {
            DealtCard(card = it, visibility = CardVisibility.PUBLIC)
        }
    }
}
